package org.mastodon.grapher.opengl.overlays;

import static org.mastodon.grapher.opengl.overlays.DataPointsOverlay.DEFAULT_POINT_SIZE;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.collection.RefSet;
import org.mastodon.grapher.opengl.PointCloudPanel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.FocusModel;
import org.mastodon.model.SelectionModel;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.util.Behaviours;

/**
 * Behaviour to select a vertex with a mouse click.
 * <p>
 * Always selects the vertex closest to the click.
 */
public class ClickSelectionBehaviour extends AbstractSelectionBehaviour implements ClickBehaviour
{
	public static final String CLICK_SELECT = "click selection";

	public static final String CLICK_ADD_SELECT = "click add to selection";

	private static final String[] CLICK_SELECT_KEYS = new String[] { "button1" };

	private static final String[] CLICK_ADD_SELECT_KEYS = new String[] { "shift button1" };

	public ClickSelectionBehaviour(
			final String name,
			final SelectionModel< Spot, Link > selection,
			final FocusModel< Spot > focus,
			final ModelGraph graph,
			final PointCloudPanel pointCloudPanel,
			final ReentrantReadWriteLock lock,
			final boolean addToSelection )
	{
		super( name, selection, focus, graph, pointCloudPanel, lock, addToSelection );
	}

	/**
	 * Coordinates of the click in layout space.
	 */
	private float centerX;
	/**
	 * Coordinates of the click in layout space.
	 */
	private float centerY;
	/**
	 * Coordinates of the bounding box in layout space.
	 */
	private float bboxXMin;
	/**
	 * Coordinates of the bounding box in layout space.
	 */
	private float bboxYMin;
	/**
	 * Coordinates of the bounding box in layout space.
	 */
	private float bboxXMax;
	/**
	 * Coordinates of the bounding box in layout space.
	 */
	private float bboxYMax;

	@Override
	public void doSelection()
	{
		final RefSet< Spot > spotsWithinBoundingBox = getSpotsWithinBoundingBox( pointCloudPanel.getDataLayout(), bboxXMin, bboxYMin, bboxXMax, bboxYMax );
		Spot closestSpot = null;
		double closestDistance = Double.MAX_VALUE;
		// Find the vertex closest to the click
		for ( final Spot spot : spotsWithinBoundingBox )
		{
			double xValue = pointCloudPanel.getDataLayout().getXFeatureValue( spot );
			double yValue = pointCloudPanel.getDataLayout().getYFeatureValue( spot );
			final double dx = centerX - xValue;
			final double dy = centerY - yValue;
			final double distanceSquared = dx * dx + dy * dy;
			if ( distanceSquared < closestDistance )
			{
				closestDistance = distanceSquared;
				closestSpot = spot;
			}
		}
		if ( closestSpot != null )
		{
			if ( addToSelection )
				selection.toggle( closestSpot );
			else
				selection.setSelected( closestSpot, true );
		}
	}

	@Override
	public void click( final int x, final int y )
	{
		updateCoordinates( x, y );
		select();
	}

	private void updateCoordinates( final int x, final int y )
	{
		screenTransformState.get( screenTransform );
		centerX = (float ) screenTransform.screenToLayoutX( x );
		centerY = (float ) screenTransform.screenToLayoutY( y );
		float halfBboxSize = DEFAULT_POINT_SIZE / 2f;
		bboxXMin = (float ) screenTransform.screenToLayoutX( x + halfBboxSize );
		bboxYMin = (float ) screenTransform.screenToLayoutY( y + halfBboxSize );
		bboxXMax = (float ) screenTransform.screenToLayoutX( x - halfBboxSize );
		bboxYMax = (float ) screenTransform.screenToLayoutY( y - halfBboxSize );
	}

	public static void install(
			final Behaviours behaviours,
			final PointCloudPanel panel,
			final ModelGraph graph,
			final FocusModel< Spot > focus,
			final SelectionModel< Spot, Link > selection,
			final ReentrantReadWriteLock lock )
	{
		final ClickSelectionBehaviour clickSelectionBehaviour = new ClickSelectionBehaviour(
				CLICK_SELECT, selection, focus, graph, panel, lock, false );
		behaviours.namedBehaviour( clickSelectionBehaviour, CLICK_SELECT_KEYS );

		final ClickSelectionBehaviour clickAddSelectionBehaviour = new ClickSelectionBehaviour(
				CLICK_ADD_SELECT, selection, focus, graph, panel, lock, true );
		behaviours.namedBehaviour( clickAddSelectionBehaviour, CLICK_ADD_SELECT_KEYS );
	}
}
