package org.elastic.jdt.ls.core.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.AbstractVMInstallType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.osgi.framework.Bundle;


public class TestVMType extends AbstractVMInstallType {

	public static final String VMTYPE_ID = "org.elastic.jdt.ls.core.internal.TestVMType";
	private static final String FAKE_JDK = "/fakejdk";
	private static final String RTSTUBS_JAR = "rtstubs.jar";

	public static void setTestJREAsDefault() throws CoreException {
		IVMInstallType vmInstallType = JavaRuntime.getVMInstallType(VMTYPE_ID);
		IVMInstall testVMInstall = vmInstallType.findVMInstall("1.8");
		if (!testVMInstall.equals(JavaRuntime.getDefaultVMInstall())) {
			// set the 1.8 test JRE as the new default JRE
			JavaRuntime.setDefaultVMInstall(testVMInstall, new NullProgressMonitor());
		}
		JDTUtils.setCompatibleVMs(VMTYPE_ID);
	}

	public TestVMType() {
		createVMInstall("1.8");
		createVMInstall("9");
		createVMInstall("10");
		createVMInstall("11");
	}

	@Override
	public String getName() {
		return "TestVMInstall-" + getId();
	}

	@Override
	public IStatus validateInstallLocation(File installLocation) {
		return Status.OK_STATUS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstallType#detectInstallLocation()
	 */
	@Override
	public File detectInstallLocation() {
		return null;
	}

	public static File getFakeJDKsLocation() {
		Bundle bundle = Platform.getBundle(JavaLanguageServerTestPlugin.PLUGIN_ID);
		try {
			URL url = FileLocator.toFileURL(bundle.getEntry(FAKE_JDK));
			File file = URIUtil.toFile(URIUtil.toURI(url));
			return file;
		} catch (IOException | URISyntaxException e) {
			ElasticJavaLanguageServerPlugin.logException(e.getMessage(), e);
			return null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.IVMInstallType#getDefaultLibraryLocations(java.io.File)
	 */
	@Override
	public LibraryLocation[] getDefaultLibraryLocations(File installLocation) {
		// for now use the same stub JAR for all
		IPath path = Path.fromOSString(new File(installLocation, RTSTUBS_JAR).getAbsolutePath());
		return new LibraryLocation[] { new LibraryLocation(path, Path.EMPTY, Path.EMPTY) };
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractVMInstallType#doCreateVMInstall(java.lang.String)
	 */
	@Override
	protected IVMInstall doCreateVMInstall(String id) {
		return new TestVMInstall(this, id);
	}

}

class TestVMInstall extends AbstractVMInstall {

	private URL javadoc;

	public TestVMInstall(IVMInstallType type, String id) {
		super(type, id);
		setNotify(false);
		setInstallLocation(new File(TestVMType.getFakeJDKsLocation(), id));
		try {
			javadoc = new URL("https://docs.oracle.com/javase/" + id.replace("1.", "") + "/docs/api/");
		} catch (MalformedURLException e) {
			ElasticJavaLanguageServerPlugin.logException(e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.launching.AbstractVMInstall#getJavadocLocation()
	 */
	@Override
	public URL getJavadocLocation() {
		return javadoc;
	}

	@Override
	public String getJavaVersion() {
		return getId();
	}
}
