package org.mastodon.grapher.opengl.overlays;

import org.scijava.ui.behaviour.DragBehaviour;

public interface SelectionBehaviour extends DragBehaviour, GLOverlayRenderer
{
	void prepareSelection();
	void doSelection();
	void finishSelection();
	default void select()
	{
		prepareSelection();
		doSelection();
		finishSelection();
	}
}
