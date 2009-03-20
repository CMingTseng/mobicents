package org.mobicents.slee.runtime.sbb;

import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.slee.SbbID;
import javax.slee.ServiceID;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPoolFactory;
import org.apache.log4j.Logger;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.container.component.SbbComponent;
import org.mobicents.slee.runtime.transaction.SleeTransactionManager;
import org.mobicents.slee.runtime.transaction.TransactionalAction;

/**
 * 
 * Manages sbb object pools in the SLEE container.
 * 
 * @author martins
 * 
 */
public class SbbObjectPoolManagement implements SbbObjectPoolManagementMBean {

	private final static Logger logger = Logger
			.getLogger(SbbObjectPoolManagement.class);

	private final ConcurrentHashMap<ObjectPoolMapKey, SbbObjectPool> pools;
	private final SleeContainer sleeContainer;

	private GenericObjectPool.Config config;

	public SbbObjectPoolManagement(SleeContainer sleeContainer) {
		this.sleeContainer = sleeContainer;
		// create pool config mbean with default pool configuration
		config = new GenericObjectPool.Config();
		config.maxActive = -1;
		config.maxIdle = 50;
		config.maxWait = -1;
		config.minEvictableIdleTimeMillis = 60000;
		config.minIdle = 0;
		config.numTestsPerEvictionRun = -1;
		config.testOnBorrow = true;
		config.testOnReturn = true;
		config.testWhileIdle = false;
		config.timeBetweenEvictionRunsMillis = 300000;
		config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
		// create pools map
		pools = new ConcurrentHashMap<ObjectPoolMapKey, SbbObjectPool>();
	}

	/**
	 * Retrieves the current pool configuration
	 * 
	 * @return
	 */
	public GenericObjectPool.Config getPoolConfig() {
		return config;
	}

	/**
	 * Defines the current pool configuration
	 */
	public void setPoolConfig(GenericObjectPool.Config config) {
		this.config = config;
	}

	/**
	 * Retrieves the object pool for the specified sbb and service.
	 * @param serviceID
	 * @param sbbID
	 * @return
	 */
	public SbbObjectPool getObjectPool(ServiceID serviceID, SbbID sbbID) {
		return pools.get(new ObjectPoolMapKey(serviceID,sbbID));
	}

	/**
	 * Creates an object pool for the specified service and sbb. If a
	 * transaction manager is used then, and if the tx rollbacks, the pool will
	 * be removed.
	 * 
	 * @param 
	 * @param sleeTransactionManager
	 */
	public void createObjectPool(final ServiceID serviceID, final SbbComponent sbbComponent,
			final SleeTransactionManager sleeTransactionManager) {

		if (logger.isDebugEnabled()) {
			logger.debug("Creating Pool for  " + serviceID +" and "+ sbbComponent);
		}

		createObjectPool(serviceID,sbbComponent);

		if (sleeTransactionManager != null) {
			// add a rollback action to remove sbb object pool
			TransactionalAction action = new TransactionalAction() {
				public void execute() {
					if (logger.isDebugEnabled()) {
						logger
								.debug("Due to tx rollback, removing pool for " + serviceID +" and "+ sbbComponent);
					}
					try {
						removeObjectPool(serviceID,sbbComponent.getSbbID());
					} catch (Throwable e) {
						logger.error("Failed to remove " + serviceID +" and "+ sbbComponent + " object pool", e);
					}
				}
			};
			try {
				sleeTransactionManager.addAfterRollbackAction(action);
			} catch (Throwable e) {
				logger.error(e.getMessage(),e);
			}
		}
	}

	/**
	 * 
	 * @param serviceID
	 * @param sbbID
	 */
	private void createObjectPool(final ServiceID serviceID, final SbbComponent sbbComponent) {
		// create the pool for the given SbbID
		GenericObjectPoolFactory poolFactory = new GenericObjectPoolFactory(
				new SbbObjectPoolFactory(serviceID,sbbComponent), config);
		final ObjectPool objectPool = poolFactory.createPool();
		final SbbObjectPool oldObjectPool = pools.put(new ObjectPoolMapKey(serviceID,sbbComponent.getSbbID()),
				new SbbObjectPool(objectPool));
		if (oldObjectPool != null) {
			// there was an old pool, close it
			try {
				oldObjectPool.close();
			} catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to close old pool for " + serviceID + "and " + sbbComponent);
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Created Pool for " + serviceID + "and " + sbbComponent);
		}
	}

	/**
	 * Removes the object pool for the sbb with the specified component and the specified service. If a
	 * transaction manager is used then, and if the tx rollbacks, the pool will
	 * be restored.
	 * 
	 * @param sbbDescriptor
	 * @param sleeTransactionManager
	 * @throws Exception
	 */
	public void removeObjectPool(final ServiceID serviceID, final SbbComponent sbbComponent,
			final SleeTransactionManager sleeTransactionManager) {

		if (logger.isDebugEnabled()) {
			logger.debug("Removing Pool for " + serviceID + "and " + sbbComponent);
		}

		removeObjectPool(serviceID,sbbComponent.getSbbID());

		if (sleeTransactionManager != null) {
			// restore object pool if tx rollbacks
			TransactionalAction action = new TransactionalAction() {
				public void execute() {
					if (logger.isDebugEnabled()) {
						logger
								.debug("Due to tx rollback, restoring pool for " + serviceID + "and " + sbbComponent);
					}
					createObjectPool(serviceID,sbbComponent);
				}
			};
			try {
				sleeTransactionManager.addAfterRollbackAction(action);
			} catch (Throwable e) {
				logger.error(e.getMessage(),e);
			}
		}
	}

	/**
	 * Removes the pool for the specified ids
	 * 
	 * @param serviceID
	 * @param sbbID
	 * @throws Exception
	 */
	private void removeObjectPool(final ServiceID serviceID, final SbbID sbbID) {
		ObjectPoolMapKey key = new ObjectPoolMapKey(serviceID,sbbID);
		final SbbObjectPool objectPool = pools.remove(key);
		if (objectPool != null) {
			try {
				objectPool.close();
			} catch (Exception e) {
				logger.error("failed to close pool",e);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Removed Pool for " + key);
		}
	}

	// ---- MBEAN METHODS

	public void register() {
		MBeanServer mBeanServer = sleeContainer.getMBeanServer();
		try {
			mBeanServer.registerMBean(this, new ObjectName(MBEAN_NAME));
		} catch (Exception e) {
			logger.error("Failed to register", e);
		}
	}

	public void unregister() {
		try {
			sleeContainer.getMBeanServer().unregisterMBean(
					new ObjectName(MBEAN_NAME));
		} catch (Exception e) {
			logger.error("Failed to unregister", e);
		}
	}

	public int getMaxActive() {
		return config.maxActive;
	}

	public void setMaxActive(int maxActive) {
		config.maxActive = maxActive;
	}

	public int getMaxIdle() {
		return config.maxIdle;
	}

	public void setMaxIdle(int maxIdle) {
		config.maxIdle = maxIdle;
	}

	public int getMinIdle() {
		return config.minIdle;
	}

	public void setMinIdle(int minIdle) {
		config.minIdle = minIdle;
	}

	public long getMaxWait() {
		return config.maxWait;
	}

	public void setMaxWait(long maxWait) {
		config.maxWait = maxWait;
	}

	public long getMinEvictableIdleTimeMillis() {
		return config.minEvictableIdleTimeMillis;
	}

	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		config.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	public int getNumTestsPerEvictionRun() {
		return config.numTestsPerEvictionRun;
	}

	public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
		config.numTestsPerEvictionRun = numTestsPerEvictionRun;
	}

	public boolean getTestOnBorrow() {
		return config.testOnBorrow;
	}

	public void setTestOnBorrow(boolean testOnBorrow) {
		config.testOnBorrow = testOnBorrow;
	}

	public boolean getTestOnReturn() {
		return config.testOnReturn;
	}

	public void setTestOnReturn(boolean testOnReturn) {
		config.testOnReturn = testOnReturn;
	}

	public boolean getTestWhileIdle() {
		return config.testWhileIdle;
	}

	public void setTestWhileIdle(boolean testWhileIdle) {
		config.testWhileIdle = testWhileIdle;
	}

	public long getTimeBetweenEvictionRunsMillis() {
		return config.timeBetweenEvictionRunsMillis;
	}

	public void setTimeBetweenEvictionRunsMillis(
			long timeBetweenEvictionRunsMillis) {
		config.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	public byte getWhenExhaustedAction() {
		return config.whenExhaustedAction;
	}

	public void setWhenExhaustedAction(byte whenExhaustedAction) {
		config.whenExhaustedAction = whenExhaustedAction;
	}

	public void reconfig() {
		for (ObjectPoolMapKey key : pools.keySet()) {
			final SbbComponent sbbComponent = sleeContainer.getComponentRepositoryImpl().getComponentByID(key.sbbID);
			createObjectPool(key.serviceID,sbbComponent);
		}
	}

	@Override
	public String toString() {
		return "SbbObject Pool Management: " + "\n+-- Pools: " + pools.keySet();
	}

	private class ObjectPoolMapKey {
		
		private final ServiceID serviceID;
		private final SbbID sbbID;
		
		public ObjectPoolMapKey(ServiceID serviceID, SbbID sbbID) {
			this.serviceID = serviceID;
			this.sbbID = sbbID;
		}
		
		@Override
		public int hashCode() {
			return serviceID.hashCode()*31+sbbID.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj != null && obj.getClass() == this.getClass()) {
				ObjectPoolMapKey other = (ObjectPoolMapKey) obj;
				return this.serviceID.equals(other.serviceID) && this.sbbID.equals(other.sbbID);
			}
			else {
				return false;
			}
		}
		
		@Override
		public String toString() {
			return serviceID.toString() + " & "+sbbID.toString();
		}
	}
}
