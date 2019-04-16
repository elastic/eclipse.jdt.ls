package org.elastic.jdt.ls.core.internal.ant;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.ExtensionPoint;
import org.apache.tools.ant.Target;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.jface.text.IRegion;

public class AntTargetNode extends AntElementNode {

	private Target fTarget = null;
	private String fLabel = null;
	private boolean isExtension = false;

	// use newAntTargetNode() instead
	private AntTargetNode(Target target) {
		super("target"); //$NON-NLS-1$
		fTarget = target;
	}

	// use newAntTargetNode() instead
	private AntTargetNode(ExtensionPoint target) {
		super("extension-point"); //$NON-NLS-1$
		fTarget = target;
		isExtension = true;
	}

	public Target getTarget() {
		return fTarget;
	}

	public boolean isDefaultTarget() {
		String targetName = fTarget.getName();
		if (targetName == null) {
			return false;
		}
		return targetName.equals(fTarget.getProject().getDefaultTarget());
	}

	/**
	 * Returns whether this target is an internal target. Internal targets are targets which has no description or starts with hyphen ('-') character.
	 * The default target is never considered internal.
	 * 
	 * @return whether the given target is an internal target
	 */
	public boolean isInternal() {
		Target target = getTarget();
		return (target.getDescription() == null || getTargetName().startsWith("-")) && !isDefaultTarget(); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ant.internal.ui.editor.model.AntElementNode#reset()
	 */
	@Override
	public void reset() {
		super.reset();
		Map<String, Target> currentTargets = fTarget.getProject().getTargets();
		if (currentTargets.get(fTarget.getName()) != null) {
			currentTargets.remove(fTarget.getName());
		}
	}

	/**
	 * Returns the name of a missing dependency or <code>null</code> if all dependencies exist in the project.
	 */
	public String checkDependencies() {
		Enumeration<String> dependencies = fTarget.getDependencies();
		while (dependencies.hasMoreElements()) {
			String dependency = dependencies.nextElement();
			if (fTarget.getProject().getTargets().get(dependency) == null) {
				return dependency;
			}
		}
		return null;
	}

	public String getTargetName() {
		String targetName = fTarget.getName();
		if (targetName == null) {
			targetName = "target"; //$NON-NLS-1$
		}
		return targetName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#containsOccurrence(java.lang.String)
	 */
	@Override
	public boolean containsOccurrence(String identifier) {
		if (getTargetName().equals(identifier)) {
			return true;
		}
		Enumeration<String> dependencies = fTarget.getDependencies();
		while (dependencies.hasMoreElements()) {
			String dependency = dependencies.nextElement();
			if (dependency.equals(identifier)) {
				return true;
			}
		}
		// looking for properties
		if (identifier.startsWith("${") && identifier.endsWith("}")) { //$NON-NLS-1$ //$NON-NLS-2$
			String ifString = fTarget.getIf();
			if (ifString != null && ifString.endsWith(identifier.substring(2, identifier.length() - 1))) {
				return true;
			}
			String unlessString = fTarget.getUnless();
			if (unlessString != null && unlessString.endsWith(identifier.substring(2, identifier.length() - 1))) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#getOccurrencesIdentifier()
	 */
	@Override
	public String getOccurrencesIdentifier() {
		return getTargetName();
	}

	@Override
	public List<Integer> computeIdentifierOffsets(String identifier) {
		String textToSearch = getAntModel().getText(getOffset(), getLength());
		if (textToSearch == null || textToSearch.length() == 0 || identifier.length() == 0) {
			return null;
		}
		List<Integer> results = new ArrayList<>();
		if (getTargetName().equals(identifier)) {
			int nameOffset = textToSearch.indexOf(IAntCoreConstants.NAME);
			nameOffset = textToSearch.indexOf(identifier, nameOffset);
			results.add(Integer.valueOf(getOffset() + nameOffset));
		} else {
			String ifString = fTarget.getIf();
			if (ifString != null && ifString.endsWith(identifier)) {
				int ifOffset = textToSearch.indexOf("if"); //$NON-NLS-1$
				ifOffset = textToSearch.indexOf(identifier, ifOffset);
				results.add(Integer.valueOf(getOffset() + ifOffset));
			} else {
				String unlessString = fTarget.getUnless();
				if (unlessString != null && unlessString.endsWith(identifier)) {
					int unlessOffset = textToSearch.indexOf("unless"); //$NON-NLS-1$
					unlessOffset = textToSearch.indexOf(identifier, unlessOffset);
					results.add(Integer.valueOf(getOffset() + unlessOffset));
				} else {
					int dependsOffset = textToSearch.indexOf("depends"); //$NON-NLS-1$
					while (dependsOffset > 0 && !Character.isWhitespace(textToSearch.charAt(dependsOffset - 1))) {
						dependsOffset = textToSearch.indexOf("depends", dependsOffset + 1); //$NON-NLS-1$
					}
					if (dependsOffset != -1) {
						dependsOffset += 7;
						int dependsOffsetEnd = textToSearch.indexOf('"', dependsOffset);
						dependsOffsetEnd = textToSearch.indexOf('"', dependsOffsetEnd + 1);
						while (dependsOffset < dependsOffsetEnd) {
							dependsOffset = textToSearch.indexOf(identifier, dependsOffset);
							if (dependsOffset == -1 || dependsOffset > dependsOffsetEnd) {
								break;
							}
							char delimiter = textToSearch.charAt(dependsOffset - 1);
							if (delimiter == ',' || delimiter == '"' || delimiter == ' ') {
								results.add(Integer.valueOf(getOffset() + dependsOffset));
							}
							dependsOffset += identifier.length();
						}
					}
				}
			}
		}
		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#isRegionPotentialReference(org.eclipse.jface.text.IRegion)
	 */
	@Override
	public boolean isRegionPotentialReference(IRegion region) {
		boolean superOK = super.isRegionPotentialReference(region);
		if (!superOK) {
			return false;
		}

		String textToSearch = getAntModel().getText(getOffset(), getLength());
		if (textToSearch == null) {
			return false;
		}
		if (checkReferenceRegion(region, textToSearch, "depends")) { //$NON-NLS-1$
			return true;
		} else if (checkReferenceRegion(region, textToSearch, IAntCoreConstants.NAME)) {
			return true;
		} else if (checkReferenceRegion(region, textToSearch, "if")) { //$NON-NLS-1$
			return true;
		}
		return checkReferenceRegion(region, textToSearch, "unless"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ant.internal.ui.model.AntElementNode#isFromDeclaration(org.eclipse.jface.text.IRegion)
	 */
	@Override
	public boolean isFromDeclaration(IRegion region) {
		String textToSearch = getAntModel().getText(getOffset(), getLength());
		if (textToSearch == null || textToSearch.length() == 0) {
			return false;
		}
		return checkReferenceRegion(region, textToSearch, IAntCoreConstants.NAME);
	}

	/**
	 * @return if this node is an extension point
	 * 
	 */
	public boolean isExtensionPoint() {
		return isExtension;
	}

	/**
	 * This function should be used to construct the AntTargetNode
	 * 
	 * @param newTarget
	 * @return newly constructed AntTargetNode
	 * 
	 */
	public static AntTargetNode newAntTargetNode(Target newTarget) {
		AntTargetNode targetNode;
		if (newTarget instanceof ExtensionPoint) {
			targetNode = new AntTargetNode((ExtensionPoint) newTarget);
		} else {
			targetNode = new AntTargetNode(newTarget);
		}
		return targetNode;
	}
}