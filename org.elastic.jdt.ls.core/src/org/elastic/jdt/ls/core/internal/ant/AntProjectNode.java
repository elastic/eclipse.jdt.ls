package org.elastic.jdt.ls.core.internal.ant;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Project;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IRegion;

public class AntProjectNode extends AntElementNode {

	protected AntModelProject fProject;
	protected IAntModel fModel;
	protected String fLabel;

	public AntProjectNode(AntModelProject project, IAntModel antModel) {
		super("project"); //$NON-NLS-1$
		fProject = project;
		fModel = antModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getLabel()
	 */
	@Override
	public String getLabel() {
		if (fLabel == null) {
			if (fProject != null) {
				fLabel = fProject.getName();
			} else {
				fLabel = "<Unable to parse as an Ant buildfile>";
			}
			if (fLabel == null || fLabel.length() == 0) {
				fLabel = "project"; //$NON-NLS-1$
			}
		}
		return fLabel;
	}

	/**
	 * Returns the Ant project associated with this project node.
	 * 
	 * @return the Ant project
	 */
	public Project getProject() {
		return fProject;
	}

	@Override
	protected IAntModel getAntModel() {
		return fModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ant.internal.ui.editor.model.AntElementNode#reset()
	 */
	@Override
	public void reset() {
		super.reset();
		fProject.reset();
		setProblemSeverity(-1);
		setProblemMessage(null);
		fOffset = -1;
		fLength = -1;
	}

	public String getDescription() {
		if (fProject == null) {
			return "<Unable to parse as an Ant buildfile>";
		}
		return fProject.getDescription();
	}

	public String getBuildFileName() {
		LocationProvider locationProvider = getAntModel().getLocationProvider();
		if (locationProvider != null) {
			IFile file = locationProvider.getFile();
			if (file != null) {
				return file.getFullPath().toOSString();
			}
		}
		return null;
	}

	public String getDefaultTargetName() {
		return fProject.getDefaultTarget();
	}

	/**
	 * @param node
	 *            the property node that is currently being configured
	 */
	public void setCurrentConfiguringProperty(AntPropertyNode node) {
		AntModelProject project = (AntModelProject) getProject();
		project.setCurrentConfiguringProperty(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getProjectNode()
	 */
	@Override
	public AntProjectNode getProjectNode() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#containsOccurrence(java.lang.String)
	 */
	@Override
	public boolean containsOccurrence(String identifier) {
		return identifier.equals(getDefaultTargetName());
	}

	@Override
	public List<Integer> computeIdentifierOffsets(String identifier) {
		String textToSearch = getAntModel().getText(getOffset(), getLength());
		if (textToSearch == null || textToSearch.length() == 0 || identifier.length() == 0) {
			return null;
		}
		List<Integer> results = new ArrayList<>(1);
		String newidentifier = new StringBuffer("\"").append(identifier).append('"').toString(); //$NON-NLS-1$
		int defaultTargetNameOffset = textToSearch.indexOf(IAntCoreConstants.DEFAULT);
		defaultTargetNameOffset = textToSearch.indexOf(newidentifier, defaultTargetNameOffset);
		results.add(Integer.valueOf(getOffset() + defaultTargetNameOffset + 1));
		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#isRegionPotentialReference(org.eclipse.jface.text.IRegion)
	 */
	@Override
	public boolean isRegionPotentialReference(IRegion region) {
		if (!super.isRegionPotentialReference(region)) {
			return false;
		}

		String textToSearch = getAntModel().getText(getOffset(), getLength());
		if (textToSearch == null) {
			return false;
		}

		return checkReferenceRegion(region, textToSearch, "default"); //$NON-NLS-1$
	}
}