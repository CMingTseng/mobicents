package org.mobicents.slee.container.component.deployment;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import javax.slee.SLEEException;
import javax.slee.management.DeploymentException;
import javax.slee.management.LibraryID;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.component.EventTypeComponent;
import org.mobicents.slee.container.component.LibraryComponent;
import org.mobicents.slee.container.component.ProfileSpecificationComponent;
import org.mobicents.slee.container.component.ResourceAdaptorComponent;
import org.mobicents.slee.container.component.ResourceAdaptorTypeComponent;
import org.mobicents.slee.container.component.SbbComponent;
import org.mobicents.slee.container.component.SleeComponent;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.EventTypeDescriptorFactory;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.EventTypeDescriptorImpl;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.LibraryDescriptorFactory;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.LibraryDescriptorImpl;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.ProfileSpecificationDescriptorFactory;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.ProfileSpecificationDescriptorImpl;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.ResourceAdaptorDescriptorFactory;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.ResourceAdaptorDescriptorImpl;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.ResourceAdaptorTypeDescriptorFactory;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.ResourceAdaptorTypeDescriptorImpl;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.SbbDescriptorFactory;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.SbbDescriptorImpl;
import org.mobicents.slee.container.component.deployment.jaxb.descriptors.library.MJar;

/**
 * DU Component jar builder
 * 
 * @author martins
 * 
 */
public class DeployableUnitJarComponentBuilder {

	private static final Logger logger = Logger
			.getLogger(DeployableUnitJarComponentBuilder.class);

	/**
	 * Builds the DU component from a jar with the specified file name,
	 * contained in the specified DU jar file. The component is built in the
	 * specified deployment dir.
	 * 
	 * @param componentJarFileName
	 * @param deployableUnitJar
	 * @param deploymentDir
	 * @param documentBuilder
	 * @return
	 * @throws DeploymentException
	 */
	public List<SleeComponent> buildComponents(String componentJarFileName,
			JarFile deployableUnitJar, File deploymentDir) throws DeploymentException {

		// extract the component jar from the DU jar, to the temp du dir
		File extractedFile = extractFile(componentJarFileName,
				deployableUnitJar, deploymentDir);
		JarFile componentJarFile = null;
		try {
			componentJarFile = new JarFile(extractedFile);
		} catch (IOException e) {
			throw new DeploymentException(
					"failed to create jar file for extracted file "
					+ extractedFile);
		}

		InputStream componentDescriptorInputStream = null;
		List<SleeComponent> components = new ArrayList<SleeComponent>();

		try {
			// now extract the jar file to a new dir
			File componentJarDeploymentDir = new File(deploymentDir,
					componentJarFileName+"-contents");
			if (!componentJarDeploymentDir.exists()) {
				if (!componentJarDeploymentDir.mkdir()) {
					throw new SLEEException("dir for jar " + componentJarFileName
							+ " not created in " + deploymentDir);
				}
			} else {
				throw new SLEEException("dir for jar " + componentJarFileName
						+ " already exists in " + deploymentDir);
			}
			extractJar(componentJarFile, componentJarDeploymentDir);
			// create components from descriptor
			JarEntry componentDescriptor = null;		
			if ((componentDescriptor = componentJarFile
					.getJarEntry("META-INF/sbb-jar.xml")) != null) {
				componentDescriptorInputStream = componentJarFile
				.getInputStream(componentDescriptor);
				SbbDescriptorFactory descriptorFactory = new SbbDescriptorFactory();
				List<SbbDescriptorImpl> descriptors = descriptorFactory.parse(componentDescriptorInputStream);
				for (SbbDescriptorImpl descriptor : descriptors) {
					SbbComponent component = new SbbComponent(descriptor);
					component.setDeploymentDir(componentJarDeploymentDir);
					components.add(component);
				}
			} else if ((componentDescriptor = componentJarFile
					.getJarEntry("META-INF/profile-spec-jar.xml")) != null) {
				componentDescriptorInputStream = componentJarFile
				.getInputStream(componentDescriptor);
				ProfileSpecificationDescriptorFactory descriptorFactory = new ProfileSpecificationDescriptorFactory();
				List<ProfileSpecificationDescriptorImpl> descriptors = descriptorFactory.parse(componentDescriptorInputStream);
				for (ProfileSpecificationDescriptorImpl descriptor : descriptors) {
					ProfileSpecificationComponent component = new ProfileSpecificationComponent(descriptor);
					component.setDeploymentDir(componentJarDeploymentDir);
					components.add(component);
				}
			} else if ((componentDescriptor = componentJarFile
					.getJarEntry("META-INF/library-jar.xml")) != null) {
				componentDescriptorInputStream = componentJarFile
				.getInputStream(componentDescriptor);
				LibraryDescriptorFactory descriptorFactory = new LibraryDescriptorFactory();
				List<LibraryDescriptorImpl> descriptors = descriptorFactory.parse(componentDescriptorInputStream);
				for (LibraryDescriptorImpl descriptor : descriptors) {
					LibraryComponent component = new LibraryComponent(descriptor);
					// create temp dir to hold all classes of the jars that this library refers
					File tempLibraryDir = createTempLibraryDeploymentDir(componentJarDeploymentDir,component.getLibraryID());
					// for each referenced jar unpack all classes
					for (MJar mJar : descriptor.getJars()) {
						// for each library component we need to unpack each referenced jar in the library component jar
						// similar process we did for component jars of the du
						File extractedLibraryFile = extractFile(mJar.getJarName(),
								componentJarFile, tempLibraryDir);
						JarFile extractedLibraryJarFile = null;
						try {
							extractedLibraryJarFile = new JarFile(extractedLibraryFile);
							extractJar(extractedLibraryJarFile, tempLibraryDir);

						} catch (IOException e) {
							throw new DeploymentException(
									"failed to create jar file for extracted file "
									+ extractedFile);
						}
						finally {
							// close library jar file
							if (extractedLibraryJarFile != null) {
								try {
									extractedLibraryJarFile.close();
								} catch (IOException e) {
									logger.error("failed to close component jar file", e);
								}
							}
							// and delete the extracted library jar file, we don't need it anymore
							if (!extractedLibraryFile.delete()) {
								logger.warn("failed to delete library " + extractedFile);
							}
						}
					}
					component.setDeploymentDir(tempLibraryDir);										
					components.add(component);
				}
			} else if ((componentDescriptor = componentJarFile
					.getJarEntry("META-INF/event-jar.xml")) != null) {
				componentDescriptorInputStream = componentJarFile.getInputStream(componentDescriptor);
				EventTypeDescriptorFactory descriptorFactory = new EventTypeDescriptorFactory();
				List<EventTypeDescriptorImpl> descriptors = descriptorFactory.parse(componentDescriptorInputStream);
				for (EventTypeDescriptorImpl descriptor : descriptors) {
					EventTypeComponent component = new EventTypeComponent(descriptor);
					component.setDeploymentDir(componentJarDeploymentDir);
					components.add(component);
				}
			} else if ((componentDescriptor = componentJarFile
					.getJarEntry("META-INF/resource-adaptor-type-jar.xml")) != null) {
				componentDescriptorInputStream = componentJarFile
				.getInputStream(componentDescriptor);
				ResourceAdaptorTypeDescriptorFactory descriptorFactory = new ResourceAdaptorTypeDescriptorFactory();
				List<ResourceAdaptorTypeDescriptorImpl> descriptors = descriptorFactory.parse(componentDescriptorInputStream);
				for (ResourceAdaptorTypeDescriptorImpl descriptor : descriptors) {
					ResourceAdaptorTypeComponent component = new ResourceAdaptorTypeComponent(descriptor);
					component.setDeploymentDir(componentJarDeploymentDir);
					components.add(component);
				}
			} else if ((componentDescriptor = componentJarFile
					.getJarEntry("META-INF/resource-adaptor-jar.xml")) != null) {
				componentDescriptorInputStream = componentJarFile
				.getInputStream(componentDescriptor);
				ResourceAdaptorDescriptorFactory descriptorFactory = new ResourceAdaptorDescriptorFactory();
				List<ResourceAdaptorDescriptorImpl> descriptors = descriptorFactory.parse(componentDescriptorInputStream);
				for (ResourceAdaptorDescriptorImpl descriptor : descriptors) {
					ResourceAdaptorComponent component = new ResourceAdaptorComponent(descriptor);
					component.setDeploymentDir(componentJarDeploymentDir);
					components.add(component);
				}
			} else {
				throw new DeploymentException(
						"No Deployment Descriptor found in the "
						+ componentJarFile.getName()
						+ " entry of a deployable unit.");
			}
		} catch (IOException e) {
			throw new DeploymentException(
					"failed to parse jar descriptor from "
							+ componentJarFile.getName(), e);
		} finally {
			if (componentDescriptorInputStream != null) {
				try {
					componentDescriptorInputStream.close();
				} catch (IOException e) {
					logger
							.error("failed to close inputstream of descriptor for jar "
									+ componentJarFile);
				}
			}
		}

		// close component jar file
		try {
			componentJarFile.close();
		} catch (IOException e) {
			logger.error("failed to close component jar file", e);
		}
		// and delete the extracted jar file, we don't need it anymore
		if (!extractedFile.delete()) {
			logger.warn("failed to delete " + extractedFile);
		}
		
		return components;
	}

	/**
	 * Extracts the file with name <code>fileName</code> out of the
	 * <code>containingJar</code> archive and stores it in <code>dstDir</code>.
	 * 
	 * @param fileName
	 *            the name of the file to extract.
	 * @param containingJar
	 *            the archive where to extract it from.
	 * @param dstDir
	 *            the location where the extracted file should be stored.
	 * @return a <code>java.io.File</code> reference to the extracted file.
	 * @throws DeploymentException
	 */
	private File extractFile(String fileName, JarFile containingJar,
			File dstDir) throws DeploymentException {

		ZipEntry zipFileEntry = containingJar.getEntry(fileName);
		logger.debug("Extracting file " + fileName + " from "
				+ containingJar.getName());
		if (zipFileEntry == null) {
			throw new DeploymentException("Error extracting jar file  "
					+ fileName + " from " + containingJar.getName());
		}
		File extractedFile = new File(dstDir, new File(zipFileEntry.getName())
				.getName());
		try {
			pipeStream(containingJar.getInputStream(zipFileEntry),
					new FileOutputStream(extractedFile));
		} catch (FileNotFoundException e) {
			throw new DeploymentException("file " + fileName + " not found in "
					+ containingJar.getName(), e);
		} catch (IOException e) {
			throw new DeploymentException("erro extracting file " + fileName
					+ " from " + containingJar.getName(), e);
		}
		logger.debug("Extracted file " + extractedFile.getName());
		return extractedFile;
	}

	/**
	 * This method will extract all the files in the jar file
	 * 
	 * @param jarFile
	 *            the jar file
	 * @param dstDir
	 *            the destination where files in the jar file be extracted
	 * @param deployableUnitID
	 * @return
	 * @throws DeploymentException
	 *             failed to extract files
	 */
	private void extractJar(JarFile jarFile, File dstDir)
			throws DeploymentException {

		// Extract jar contents to a classpath location
		JarInputStream jarIs = null;
		try {
			jarIs = new JarInputStream(new BufferedInputStream(
					new FileInputStream(jarFile.getName())));

			for (JarEntry entry = jarIs.getNextJarEntry(); jarIs.available() > 0
					&& entry != null; entry = jarIs.getNextJarEntry()) {
				logger.debug("jar entry = " + entry.getName());

				if (entry.isDirectory()) {
					// Create jar directories.
					File dir = new File(dstDir, entry.getName());
					if (!dir.exists()) {
						if (!dir.mkdirs()) {
							logger.debug("Failed to create directory "
									+ dir.getAbsolutePath());
							throw new IOException("Failed to create directory "
									+ dir.getAbsolutePath());
						}
					} else
						logger.debug("Created directory"
								+ dir.getAbsolutePath());
				} else // unzip files
				{
					File file = new File(dstDir, entry.getName());
					File dir = file.getParentFile();
					if (!dir.exists()) {
						if (!dir.mkdirs()) {
							logger.debug("Failed to create directory "
									+ dir.getAbsolutePath());
							throw new IOException("Failed to create directory "
									+ dir.getAbsolutePath());
						} else
							logger.debug("Created directory"
									+ dir.getAbsolutePath());
					}
					pipeStream(jarFile.getInputStream(entry),
							new FileOutputStream(file));

				}
			}
		} catch (Exception e) {
			throw new DeploymentException("failed to extract jar file "
					+ jarFile.getName());
		} finally {
			if (jarIs != null) {
				try {
					jarIs.close();
				} catch (IOException e) {
					logger.error("failed to close jar input stream", e);
				}
			}
		}

	}

	private static byte buffer[] = new byte[8192];

	/**
	 * Pipes data from the input stream into the output stream.
	 * 
	 * @param is
	 *            The InputStream where the data is coming from.
	 * @param os
	 *            The OutputStream where the data is going to.
	 * @throws IOException
	 *             if reading or writing the data fails.
	 */
	private void pipeStream(InputStream is, OutputStream os)
			throws IOException {
		synchronized (buffer) {
			try {
				for (int bytesRead = is.read(buffer); bytesRead != -1; bytesRead = is
						.read(buffer))
					os.write(buffer, 0, bytesRead);
				is.close();
				os.close();
			} catch (IOException ioe) {
				try {
					is.close();
				} catch (Exception ioexc) {/* do sth? */
				}
				try {
					os.close();
				} catch (Exception ioexc) {/* do sth? */
				}
				throw ioe;
			}
		}
	}
	
	/**
	 * Creates the directory that will be used for unpacking the child jars for a given library.
	 * @param rootDir
	 * @param sourceUrl
	 * @throws SLEEException if the dir can't be created
	 * @return
	 */
    private File createTempLibraryDeploymentDir(File deploymentRoot, LibraryID libraryID) {
        try {
            // first create a dummy file to gurantee uniqueness. I would have been nice if the File class had a createTempDir() method
            // IVELIN -- do not use jarName here because windows cannot see the path (exceeds system limit)
            File tempFile = File.createTempFile("mobicents-slee-library-", "", deploymentRoot);
            File tempDeploymentDir = new File(tempFile.getAbsolutePath() + "-contents");
            if (!tempDeploymentDir.exists()) {
            	tempDeploymentDir.mkdirs();
            }
            else {
            	throw new SLEEException("Dir "+tempDeploymentDir+" already exists, unable to create deployment dir for library "+libraryID);
            }
            tempFile.delete();
            return tempDeploymentDir;
        } catch (IOException e) {            
            throw new SLEEException("Failed to create deployment dir for library "+libraryID, e);
        }
    }
}
