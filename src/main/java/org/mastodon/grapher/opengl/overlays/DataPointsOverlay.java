package org.mastodon.grapher.opengl.overlays;

import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.glDisableClientState;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11C.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL15C.glBufferData;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL15C;
import org.mastodon.grapher.opengl.DataLayout;
import org.mastodon.grapher.opengl.InertialScreenTransformEventHandler;
import org.mastodon.grapher.opengl.LayoutChangeListener;
import org.mastodon.views.grapher.display.FeatureGraphConfig;
import org.scijava.listeners.Listeners;

public class DataPointsOverlay implements GLOverlayRenderer
{

	private static final int INIT_BUFFER_SIZE = 10; // 10_000;

	public static final int VERTEX_SIZE = 2; // X, Y

	public static final int COLOR_SIZE = 4; // R, G, B, alpha

	/** The maximal growth step. */
	private final int maximumGrowth = Integer.MAX_VALUE;

	private FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer( INIT_BUFFER_SIZE * VERTEX_SIZE );

	private FloatBuffer colorBuffer = BufferUtils.createFloatBuffer( INIT_BUFFER_SIZE * VERTEX_SIZE );

	private int vboVertexHandle;

	private int vboColorHandle;

	private int nPoints = 0;

	private final float pointSize = 5.1f;

	private float[] xyData;

	private float[] colorData;

	private boolean updateXY;

	private boolean updateColor;

	private final DataLayout layout;

	private final InertialScreenTransformEventHandler transformHandler;

	private final Listeners.List< LayoutChangeListener > layoutChangeListeners;

	public DataPointsOverlay( final DataLayout layout, final InertialScreenTransformEventHandler transformHandler )
	{
		this.layout = layout;
		this.transformHandler = transformHandler;
		this.layoutChangeListeners = new Listeners.SynchronizedList<>();
	}

	public Listeners.List< LayoutChangeListener > getLayoutChangeListeners()
	{
		return layoutChangeListeners;
	}

	private void putCoords( final float[] xyData )
	{
		this.xyData = xyData;
		this.updateXY = true;
		// Update min & max.
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		for ( int i = 0; i < xyData.length; i++ )
		{
			final float x = xyData[ i ];
			minX = Math.min( minX, x );
			maxX = Math.max( maxX, x );

			i++;
			final float y = xyData[ i ];
			minY = Math.min( minY, y );
			maxY = Math.max( maxY, y );
		}
		final float layoutMinX = minX;
		final float layoutMinY = minY;
		final float layoutMaxX = maxX;
		final float layoutMaxY = maxY;
		layoutChangeListeners.list.forEach( l -> l.layoutChanged( layoutMinX, layoutMaxX, layoutMinY, layoutMaxY ) );
	}

	private void putColors( final float[] colorData )
	{
		this.colorData = colorData;
		this.updateColor = true;
	}

	private void resizeBuffersForNPoints( final int desiredNPoints )
	{
		final int oldCapacity = vertexBuffer.capacity();
		if ( desiredNPoints * VERTEX_SIZE <= oldCapacity )
			return;

		final int growth = Math.min( oldCapacity / 2 + 16, maximumGrowth );
		final int growthNPoints;
		if ( growth > Integer.MAX_VALUE - oldCapacity )
			growthNPoints = Integer.MAX_VALUE / VERTEX_SIZE;
		else
			growthNPoints = ( oldCapacity + growth ) / VERTEX_SIZE;

		final int newNPoints = Math.max( desiredNPoints, growthNPoints );

		// Make new buffers.
		this.vertexBuffer = BufferUtils.createFloatBuffer( newNPoints * VERTEX_SIZE );
		this.colorBuffer = BufferUtils.createFloatBuffer( newNPoints * COLOR_SIZE );

		// Link new buffers.
		this.vboVertexHandle = GL15C.glGenBuffers();
		glBindBuffer( GL_ARRAY_BUFFER, vboVertexHandle );
		glBufferData( GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW );
		glBindBuffer( GL_ARRAY_BUFFER, 0 );
		this.vboColorHandle = GL15C.glGenBuffers();
		glBindBuffer( GL_ARRAY_BUFFER, vboColorHandle );
		glBufferData( GL_ARRAY_BUFFER, colorBuffer, GL_STATIC_DRAW );
		glBindBuffer( GL_ARRAY_BUFFER, 0 );
	}

	@Override
	public void init()
	{
		// Make new buffers.
		this.vertexBuffer = BufferUtils.createFloatBuffer( INIT_BUFFER_SIZE * VERTEX_SIZE );
		this.colorBuffer = BufferUtils.createFloatBuffer( INIT_BUFFER_SIZE * COLOR_SIZE );

		// New handles.
		this.vboVertexHandle = GL15C.glGenBuffers();
		this.vboColorHandle = GL15C.glGenBuffers();
	}

	@Override
	public void paint()
	{
		glPointSize( pointSize );

		if ( updateXY )
		{
			if ( xyData.length > vertexBuffer.capacity() )
				resizeBuffersForNPoints( xyData.length / 2 );

			vertexBuffer.limit( xyData.length );
			vertexBuffer.put( xyData );
			vertexBuffer.flip();
			nPoints = xyData.length / 2;
			updateXY = false;
		}
		if ( updateColor )
		{
			colorBuffer.limit( colorData.length );
			colorBuffer.put( colorData );
			colorBuffer.flip();
			updateColor = false;
		}

		glEnableClientState( GL11.GL_VERTEX_ARRAY );
		glEnableClientState( GL_COLOR_ARRAY );

		glBindBuffer( GL15.GL_ARRAY_BUFFER, vboVertexHandle );
		GL15.glBufferSubData( GL15.GL_ARRAY_BUFFER, 0, vertexBuffer );
		GL11.glVertexPointer( VERTEX_SIZE, GL11.GL_FLOAT, 0, 0 );

		glBindBuffer( GL_ARRAY_BUFFER, vboColorHandle );
		GL15.glBufferSubData( GL15.GL_ARRAY_BUFFER, 0, colorBuffer );
		GL15.glColorPointer( COLOR_SIZE, GL11.GL_FLOAT, 0, 0 );

		glDrawArrays( GL11.GL_POINTS, 0, nPoints );
		glBindBuffer( GL15.GL_ARRAY_BUFFER, 0 );

		glDisableClientState( GL_COLOR_ARRAY );
		glDisableClientState( GL_VERTEX_ARRAY );
	}

	public void plot( final FeatureGraphConfig graphConfig )
	{
		final float[] xy = layout.layout();
		final float[] color = layout.color();
		putCoords( xy );
		putColors( color );
		transformHandler.layoutChanged( xy );
	}
	
	public void updateColors()
	{
		final float[] color = layout.color();
		putColors( color );
	}
}
