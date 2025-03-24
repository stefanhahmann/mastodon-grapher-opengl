package org.mastodon.grapher.opengl.overlays;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mastodon.collection.RefSet;
import org.mastodon.grapher.opengl.DataLayoutMaker;
import org.mastodon.grapher.opengl.PointCloudPanel;
import org.mastodon.mamut.model.Link;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.model.FocusModel;
import org.mastodon.model.SelectionModel;
import org.mastodon.views.grapher.datagraph.ScreenTransform;
import org.mastodon.views.grapher.display.ScreenTransformState;
import org.scijava.ui.behaviour.util.AbstractNamedBehaviour;

public abstract class AbstractSelectionBehaviour extends AbstractNamedBehaviour implements SelectionBehaviour
{
	protected final SelectionModel< Spot, Link > selection;

	private final ScreenTransformState screenTransformState;

	protected final ScreenTransform screenTransform;

	protected final PointCloudPanel pointCloudPanel;

	private final ReentrantReadWriteLock lock;

	protected final ModelGraph graph;

	protected final FocusModel< Spot > focus;

	private final boolean addToSelection;

	protected boolean dragging = false;

	AbstractSelectionBehaviour( final String name, final SelectionModel< Spot, Link > selection, final FocusModel<Spot> focus,
			final ModelGraph graph, final PointCloudPanel pointCloudPanel,
			final ReentrantReadWriteLock lock, final boolean addToSelection )
	{
		super( name );
		this.selection = selection;
		this.focus = focus;
		this.graph = graph;
		this.pointCloudPanel = pointCloudPanel;
		this.lock = lock;
		this.addToSelection = addToSelection;
		this.screenTransformState = pointCloudPanel.getScreenTransform();
		this.screenTransform = new ScreenTransform();
		pointCloudPanel.getCanvas().overlays().add( this );
	}

	protected abstract void doDrag( final int x, final int y );

	protected abstract void doInit( final int x, final int y );

	protected void doEnd(final int x, final int y)
	{
		// do nothing
	}

	@Override
	public void init( final int x, final int y )
	{
		screenTransformState.get( screenTransform );
		doInit( x, y );
		dragging = false;
		pointCloudPanel.overlayChanged();
	}

	@Override
	public void drag( final int x, final int y )
	{
		screenTransformState.get( screenTransform );
		doDrag( x, y );
		if ( !dragging )
			dragging = true;
		pointCloudPanel.overlayChanged();
	}

	@Override
	public void end( final int x, final int y )
	{
		doEnd( x, y );
		if ( dragging )
		{
			dragging = false;
			lock.readLock().lock();
			try
			{
				select();
			}
			finally
			{
				lock.readLock().unlock();
			}
		}
		else
		{
			selection.clearSelection();
		}
		pointCloudPanel.overlayChanged();
	}

	@Override
	public void prepareSelection( )
	{
		selection.pauseListeners();
		if ( !addToSelection )
			selection.clearSelection();
	}

	@Override
	public void finishSelection( )
	{
		selection.resumeListeners();
	}

	protected RefSet< Spot > getSpotsWithinBoundingBox( final DataLayoutMaker layout, final float x1, final float y1, final float x2, final float y2 )
	{
		return layout.getSpotWithin( x1, y1, x2, y2 );
	}
}
