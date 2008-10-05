/*
 * Copyright (c) 2006 jDiameter.
 * https://jdiameter.dev.java.net/
 *
 * License: Lesser General Public License (LGPL)
 *
 * e-mail: erick.svenson@yahoo.com
 *
 */
package org.jdiameter.server.impl.app.acc;

import org.jdiameter.api.*;
import org.jdiameter.api.acc.ServerAccSession;
import org.jdiameter.api.acc.ServerAccSessionListener;
import org.jdiameter.api.acc.events.AccountAnswer;
import org.jdiameter.api.acc.events.AccountRequest;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.app.StateEvent;
import org.jdiameter.common.api.app.IAppSessionState;
import org.jdiameter.common.api.app.acc.IServerAccActionContext;
import org.jdiameter.common.api.app.acc.ServerAccSessionState;
import static org.jdiameter.common.api.app.acc.ServerAccSessionState.IDLE;
import static org.jdiameter.common.api.app.acc.ServerAccSessionState.OPEN;
import org.jdiameter.common.impl.app.acc.AccountRequestImpl;
import org.jdiameter.common.impl.app.acc.AppAccSessionImpl;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class ServerAccSessionImpl extends AppAccSessionImpl implements ServerAccSession, NetworkReqListener {

    protected ServerAccSessionState state = ServerAccSessionState.IDLE;
    protected IServerAccActionContext context;
    protected long tsTimeout;
    protected ScheduledFuture tsTask;
    protected ServerAccSessionListener listener;
    protected boolean stateless = false;

    //protected Logger logger =Logger.getLogger(ServerAccSessionImpl.class.getCanonicalName());
    
    public ServerAccSessionImpl(Session session, Request initialRequest, ServerAccSessionListener listener, long tsTimeout, boolean stateless, StateChangeListener... scListeners) {
        if (session == null)
            throw new IllegalArgumentException("Session can not be null");
        if (listener == null)
            throw new IllegalArgumentException("Session listener can not be null");
        this.session = session;
        this.listener = listener;
        if (this.listener instanceof IServerAccActionContext) {
            context = (IServerAccActionContext) this.listener;
        }
        this.tsTimeout = tsTimeout;
        this.stateless = stateless;
        this.session.setRequestListener(this);
        for (StateChangeListener l : scListeners)
            addStateChangeNotification(l);
        //processRequest(initialRequest);
    }

    public void sendAccountAnswer(AccountAnswer accountAnswer) throws InternalException, IllegalStateException, RouteException, OverloadException {
        try {
            session.send(accountAnswer.getMessage());
        } catch (IllegalDiameterStateException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean isStateless() {
        return stateless;
    }

    protected void setState(IAppSessionState newState) {
        IAppSessionState oldState = state;
        state = (ServerAccSessionState) newState;
        for (StateChangeListener i : stateListeners)
            i.stateChanged( (Enum) oldState, (Enum) newState);
    }

    public boolean handleEvent(StateEvent event) throws InternalException, OverloadException {
        return stateless ? handleEventForStatelesMode(event) : handleEventForStatefullMode(event);
    }

    public boolean handleEventForStatelesMode(StateEvent event) throws InternalException, OverloadException {       
        try {
            switch (state) {
                // Idle ==========
                case IDLE: {
                    switch ((Event.Type) event.getType()) {
                        case RECIVED_START_RECORD :
                            if (listener != null)
                                try {
                                    listener.doAccRequestEvent(this, (AccountRequest)event.getData());
                                } catch (Exception exc) {
                                    logger.debug(exc);
                                }
                            setState(IDLE);
                            break;
                        case RECIVED_EVENT_RECORD :
                            if (listener != null)
                                try {
                                    listener.doAccRequestEvent(this, (AccountRequest)event.getData());
                                } catch (Exception exc) {
                                    logger.debug(exc);
                                }
                            setState(IDLE);
                            break;
                        case RECIVED_INTERIM_RECORD :
                            if (listener != null)
                                try {
                                    listener.doAccRequestEvent(this, (AccountRequest)event.getData());
                                } catch (Exception exc) {
                                    logger.debug(exc);
                                }
                            setState(IDLE);
                            break;
                        case RECIVED_STOP_RECORD :
                            if (listener != null)
                                try {
                                    listener.doAccRequestEvent(this, (AccountRequest)event.getData());
                                } catch (Exception exc) {
                                    logger.debug(exc);
                                }
                            setState(IDLE);
                            break;
                        default:
                            throw new IllegalStateException("Current state " + state + " action " + event.getType());
                    }
                }
            }
        } catch (Exception exc) {
            logger.debug(exc);
            return false;
        }
        return true;
    }

    public boolean handleEventForStatefullMode(StateEvent event) throws InternalException, OverloadException {
        try {
            switch (state) {
                // Idle ==========
                case IDLE: {
                    switch ((Event.Type) event.getType()) {
                        case RECIVED_START_RECORD :
                            if (listener != null) {
                                try {
                                    listener.doAccRequestEvent(this, (AccountRequest)event.getData());
                                    tsTask = runTsTimer();
                                    if (context != null)
                                        context.sessionTimerStarted(this, tsTask);
                                    setState(OPEN);
                                } catch (Exception exc) {
                                    logger.debug(exc);
                                    setState(IDLE);
                                }
                            }
                            break;
                        case RECIVED_EVENT_RECORD :
                            if (listener != null) {
                                try {
                                    listener.doAccRequestEvent(this, (AccountRequest)event.getData());
                                } catch (Exception exc) {
                                    logger.debug(exc);
                                }
                            }                            
                            break;
                    }
                    break;
                }
                case OPEN : {
                    switch ((Event.Type) event.getType()) {
                        case RECIVED_INTERIM_RECORD :
                            try {
                                listener.doAccRequestEvent(this, (AccountRequest) event.getData());
                                tsTask = runTsTimer();
                                if (context != null)
                                    context.sessionTimerStarted(this, tsTask);
                            } catch (Exception exc) {
                                logger.debug(exc);
                                setState(IDLE);
                            }
                            break;
                        case RECIVED_STOP_RECORD :
                            try {
                                listener.doAccRequestEvent(this, (AccountRequest) event.getData());
                                tsTask.cancel(true);
                                if (context != null)
                                    context.srssionTimerCanceled(this, tsTask);
                                setState(IDLE);
                            } catch (Exception exc) {
                                logger.debug(exc);
                                setState(IDLE);
                            }
                            break;
                    }
                    break;
                }
            }
        } catch (Exception exc) {
            logger.debug(exc);
            return false;
        }
        return true;
    }

    private ScheduledFuture runTsTimer() {
        return scheduler.schedule(new Runnable() {
            public void run() {
                logger.debug("Ts timer expired");
                if (context != null)
                    try {
                        context.sessionTimeoutElapses(ServerAccSessionImpl.this);
                    } catch (InternalException e) {
                        logger.debug(e);
                    }
                setState(IDLE);
            }
        }, tsTimeout, TimeUnit.MILLISECONDS);
    }

    protected Answer createStopAnswer(Request request) {
        Answer answer = request.createAnswer(ResultCode.SUCCESS);
        answer.getAvps().addAvp(Avp.ACC_RECORD_TYPE, 4);
        answer.getAvps().addAvp(request.getAvps().getAvp(Avp.ACC_RECORD_NUMBER));
        return answer;
    }

    protected Answer createInterimAnswer(Request request) {
        Answer answer = request.createAnswer(ResultCode.SUCCESS);
        answer.getAvps().addAvp(Avp.ACC_RECORD_TYPE, 3);
        answer.getAvps().addAvp(request.getAvps().getAvp(Avp.ACC_RECORD_NUMBER));
        return answer;
    }

    protected Answer createEventAnswer(Request request) {
        Answer answer = request.createAnswer(ResultCode.SUCCESS);
        answer.getAvps().addAvp(Avp.ACC_RECORD_TYPE, 2);
        answer.getAvps().addAvp(request.getAvps().getAvp(Avp.ACC_RECORD_NUMBER));
        return answer;
    }

    protected Answer createStartAnswer(Request request) {
        Answer answer = request.createAnswer(ResultCode.SUCCESS);
        answer.getAvps().addAvp(Avp.ACC_RECORD_TYPE, 1);
        answer.getAvps().addAvp(request.getAvps().getAvp(Avp.ACC_RECORD_NUMBER));
        return answer;
    }

    public <E> E getState(Class<E> eClass) {
        return eClass == ServerAccSessionState.class ? (E) state : null;
    }

    public Answer processRequest(Request request) {        
        if (request.getCommandCode() == AccountRequestImpl.code) {
            try {
                sendAndStateLock.lock();
                handleEvent(new Event(createAccountRequest(request)));
            } catch (Exception e) {
                logger.debug(e);
            } finally {
                sendAndStateLock.unlock();
            }
        } else {
            try {
                listener.doOtherEvent(this, createAccountRequest(request), null); 
            } catch (Exception e) {
                logger.debug(e);
            }
        }
        return null;
    }

}
