package bigwarp;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.util.ArrayUtil;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;

import jitk.spline.XfmUtils;

import org.janelia.utility.ui.RepeatingReleasedEventsFixer;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.converter.ComplexPowerGLogFloatConverter;
import net.imglib2.display.RealARGBColorConverter;
import net.imglib2.exception.ImgLibException;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.img.imageplus.ImagePlusImgFactory;
import net.imglib2.img.imageplus.ImagePlusImgs;
import net.imglib2.img.imageplus.IntImagePlus;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.ui.InteractiveDisplayCanvasComponent;
import net.imglib2.ui.PainterThread;
import net.imglib2.ui.RenderTarget;
import net.imglib2.ui.TransformEventHandler;
import net.imglib2.ui.TransformListener;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import mpicbg.models.AbstractModel;
import mpicbg.models.AffineModel2D;
import mpicbg.models.AffineModel3D;
import mpicbg.models.CoordinateTransform;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.RigidModel2D;
import mpicbg.models.RigidModel3D;
import mpicbg.models.SimilarityModel2D;
import mpicbg.spim.data.SpimDataException;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import bdv.ViewerImgLoader;
import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import bdv.gui.BigWarpLandmarkPanel;
import bdv.gui.BigWarpViewerFrame;
import bdv.gui.LandmarkKeyboardProcessor;
import bdv.ij.BigWarpImagePlusPlugIn;
import bdv.img.WarpedSource;
import bdv.img.cache.Cache;
import bdv.spimdata.SpimDataMinimal;
import bdv.spimdata.WrapBasicImgLoader;
import bdv.spimdata.XmlIoSpimDataMinimal;
import bdv.tools.InitializeViewerState;
import bdv.tools.VisibilityAndGroupingDialog;
import bdv.tools.brightness.BrightnessDialog;
import bdv.tools.brightness.ConverterSetup;
import bdv.tools.brightness.MinMaxGroup;
import bdv.tools.brightness.RealARGBColorConverterSetup;
import bdv.tools.brightness.SetupAssignments;
import bdv.util.KeyProperties;
import bdv.viewer.BigWarpConverterSetupWrapper;
import bdv.viewer.BigWarpDragOverlay;
import bdv.viewer.BigWarpLandmarkFrame;
import bdv.viewer.BigWarpOverlay;
import bdv.viewer.BigWarpViewerPanel;
import bdv.viewer.BigWarpViewerSettings;
import bdv.viewer.Interpolation;
import bdv.viewer.LandmarkPointMenu;
import bdv.viewer.MultiBoxOverlay2d;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.ViewerOptions;
import bdv.viewer.ViewerPanel;
import bdv.viewer.VisibilityAndGrouping;
import bdv.viewer.WarpNavigationActions;
import bdv.viewer.animate.SimilarityModel3D;
import bdv.viewer.animate.TranslationAnimator;
import bdv.viewer.overlay.BigWarpSourceOverlayRenderer;
import bdv.viewer.overlay.MultiBoxOverlayRenderer;
import bdv.viewer.render.MultiResolutionRenderer;
import bdv.viewer.state.ViewerState;
import bigwarp.landmarks.LandmarkTableModel;
import bigwarp.source.GridSource;
import bigwarp.source.WarpMagnitudeSource;

public class BigWarp {
	
	protected static final int DEFAULT_WIDTH  = 600;
	protected static final int DEFAULT_HEIGHT = 400;
	
	// descriptive names for indexing sources
	protected int movingSourceIndex = 0;
	protected int fixedSourceIndex = 1;
	
	protected ArrayList< SourceAndConverter< ? > > sources;
	protected Object movingSourceType;
	protected Object targetSourceType;
	
	protected final SetupAssignments setupAssignments;
	protected final BrightnessDialog brightnessDialog;
	protected final WarpVisFrame warpVisDialog;
	protected final HelpDialog helpDialog;
	
	protected final VisibilityAndGroupingDialog activeSourcesDialog;
	
	final AffineTransform3D fixedViewXfm;
	private final BigWarpViewerFrame viewerFrameP;
	private final BigWarpViewerFrame viewerFrameQ;
	protected final BigWarpViewerPanel viewerP;
	protected final BigWarpViewerPanel viewerQ;
	protected final AffineTransform3D initialViewP;
	protected final AffineTransform3D initialViewQ;
	
	protected BigWarpLandmarkPanel landmarkPanel;
	protected final LandmarkPointMenu    landmarkPopupMenu;
	protected final BigWarpLandmarkFrame landmarkFrame;
	
	protected final BigWarpViewerSettings viewerSettings;
	protected final BigWarpOverlay overlayP;
	protected final BigWarpOverlay overlayQ;
	protected final BigWarpDragOverlay dragOverlayP;
	protected final BigWarpDragOverlay dragOverlayQ;
	
	protected double rad = 7;
	protected double LANDMARK_DOT_SIZE = 14; // diameter of dots
	
	protected RealPoint currentLandmark;	
	
	protected LandmarkTableModel landmarkModel;
	protected JTable			 landmarkTable;
	
	protected LandmarkTableListener landmarkModellistener;
	
	protected MouseLandmarkListener landmarkClickListenerP;
	protected MouseLandmarkListener landmarkClickListenerQ;
	
	protected MouseLandmarkTableListener landmarkTableListener;

	protected int ndims;
	
	protected TransformEventHandler<AffineTransform3D> handlerQ;
	protected TransformEventHandler<AffineTransform3D> handlerP;
	final static DummyTransformEventHandler DUMMY_TRANSFORM_HANDLER = new DummyTransformEventHandler();

	protected final int gridSourceIndex;
	protected final int warpMagSourceIndex;
	protected final AbstractModel<?>[] baseXfmList;
	
	private final double[] ptBack;
	/*
	 * landmarks are placed on clicks only if we are inLandmarkMode
	 * during the click
	 */
	protected boolean inLandmarkMode;
	
	// file selection
	final JFrame	 fileFrame;
	final FileDialog fileDialog;
	
	protected File lastDirectory;
	
	JMenu landmarkMenu;
	
	final ProgressWriter progressWriter;
	
	private static ImageJ ij;
	
	public BigWarp( BigWarpData data, final String windowTitle, final ProgressWriter progressWriter ) throws SpimDataException
	{
		
		sources = data.sources;
//		AbstractSequenceDescription<?, ?, ?> seq = data.seq;
		ArrayList<ConverterSetup> converterSetups = data.converterSetups;
		this.progressWriter = progressWriter;
		
		movingSourceType = data.seqP.getImgLoader().getSetupImgLoader( 0 ).getImageType();
		targetSourceType = data.seqQ.getImgLoader().getSetupImgLoader( 0 ).getImageType();

		ndims = 3;
		ndims = detectNumDims();
		ptBack = new double[ 3 ];
		
		sources = wrapSourcesAsTransformed( sources, ndims, movingSourceIndex );
		baseXfmList = new AbstractModel<?>[ 3 ];
		setupWarpMagBaselineOptions( baseXfmList, ndims );
		
		fixedViewXfm = sources.get( fixedSourceIndex ).getSpimSource().getSourceTransform( 0, 0 );
		
		warpMagSourceIndex = addWarpMagnitudeSource( sources, converterSetups, "WarpMagnitudeSource", data );
		gridSourceIndex = addGridSource( sources, converterSetups, "GridSource", data );
		setGridType( GridSource.GRID_TYPE.LINE );
		

		// If the images are 2d, use a transform handler that limits transformations to 
		// rotations and scalings of the 2d plane ( z = 0 )
		ViewerOptions optionsP = ViewerOptions.options();
		ViewerOptions optionsQ = ViewerOptions.options();
		
//		if( ndims == 2 )
//		{
//			optionsP.transformEventHandlerFactory( TransformHandler3DWrapping2D.factory() );
//			optionsQ.transformEventHandlerFactory( TransformHandler3DWrapping2D.factory() );
//			
//			optionsP.boxOverlayRenderer( new MultiBoxOverlayRenderer( DEFAULT_WIDTH, DEFAULT_HEIGHT, new MultiBoxOverlay2d()) );
//			optionsQ.boxOverlayRenderer( new MultiBoxOverlayRenderer( DEFAULT_WIDTH, DEFAULT_HEIGHT, new MultiBoxOverlay2d()) );
//		}
//		optionsP.sourceInfoOverlayRenderer( new BigWarpSourceOverlayRenderer() );
//		optionsQ.sourceInfoOverlayRenderer( new BigWarpSourceOverlayRenderer() );
		
		viewerSettings = new BigWarpViewerSettings();
		
		// Viewer frame for the moving image
		viewerFrameP = new BigWarpViewerFrame( this, DEFAULT_WIDTH, DEFAULT_HEIGHT, sources, viewerSettings,
				( ( ViewerImgLoader ) data.seqP.getImgLoader() ).getCache(), optionsP, "Bigwarp moving image", true );

		viewerP = getViewerFrameP().getViewerPanel();

		
		// Viewer frame for the fixed image
		viewerFrameQ = new BigWarpViewerFrame( this, DEFAULT_WIDTH, DEFAULT_HEIGHT, sources, viewerSettings,
				( ( ViewerImgLoader ) data.seqQ.getImgLoader() ).getCache(), optionsQ, "Bigwarp fixed image", false );

		viewerQ = getViewerFrameQ().getViewerPanel();
		
		if( ndims == 2 )
		{
			Class<ViewerPanel> c_vp = ViewerPanel.class;
			Class<? extends InteractiveDisplayCanvasComponent> c_idcc = viewerP.getDisplay().getClass();
			try 
			{
				Field overlayRendererField = c_vp.getDeclaredField( "multiBoxOverlayRenderer" );
				overlayRendererField.setAccessible( true );

				MultiBoxOverlayRenderer overlayRenderP = new MultiBoxOverlayRenderer( DEFAULT_WIDTH, DEFAULT_HEIGHT );
				MultiBoxOverlayRenderer overlayRenderQ = new MultiBoxOverlayRenderer( DEFAULT_WIDTH, DEFAULT_HEIGHT );
				Class orp = overlayRenderP.getClass();
				Field boxField = orp.getDeclaredField("box");
				boxField.setAccessible( true );
				boxField.set( overlayRenderP, new MultiBoxOverlay2d() );
				boxField.set( overlayRenderQ, new MultiBoxOverlay2d() );
				boxField.setAccessible( false );
				
				overlayRendererField.set( viewerP, overlayRenderP );
				overlayRendererField.set( viewerQ, overlayRenderQ );
				overlayRendererField.setAccessible( false );

				Field handlerField = c_idcc.getDeclaredField( "handler" );
				handlerField.setAccessible( true );

				viewerP.getDisplay().removeHandler( handlerField.get( viewerP.getDisplay() ));
				viewerQ.getDisplay().removeHandler( handlerField.get( viewerQ.getDisplay() ));

				TransformEventHandler<AffineTransform3D> pHandler = TransformHandler3DWrapping2D.factory().create( viewerP.getDisplay());
				pHandler.setCanvasSize( viewerP.getDisplay().getWidth(), viewerP.getDisplay().getHeight(), false );

				TransformEventHandler<AffineTransform3D> qHandler = TransformHandler3DWrapping2D.factory().create( viewerQ.getDisplay());
				qHandler.setCanvasSize( viewerQ.getDisplay().getWidth(), viewerQ.getDisplay().getHeight(), false );

				handlerField.set( viewerP.getDisplay(), pHandler );
				handlerField.set( viewerQ.getDisplay(), qHandler );

				viewerP.getDisplay().addHandler( pHandler );
				viewerQ.getDisplay().addHandler( qHandler );
				handlerField.setAccessible( false );

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		try
		{
			Class<ViewerPanel> c_vp = ViewerPanel.class;
			Field sourceInfoOverlayRendererField = c_vp.getDeclaredField( "sourceInfoOverlayRenderer" );
			sourceInfoOverlayRendererField.setAccessible( true );
			sourceInfoOverlayRendererField.set( viewerP, new BigWarpSourceOverlayRenderer() );
			sourceInfoOverlayRendererField.set( viewerQ, new BigWarpSourceOverlayRenderer() );
			sourceInfoOverlayRendererField.setAccessible( false );
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		viewerP.setNumDim( ndims );
		viewerQ.setNumDim( ndims );
		
		activeSourcesDialog = new VisibilityAndGroupingDialog( viewerFrameQ, viewerQ.getVisibilityAndGrouping() );
		
		// set warp mag source to inactive at the start
		viewerFrameP.getViewerPanel().getVisibilityAndGrouping().setSourceActive( warpMagSourceIndex, false );
		viewerFrameQ.getViewerPanel().getVisibilityAndGrouping().setSourceActive( warpMagSourceIndex, false );
		// set warp grid source to inactive at the start 
		viewerFrameP.getViewerPanel().getVisibilityAndGrouping().setSourceActive( gridSourceIndex, false );
		viewerFrameQ.getViewerPanel().getVisibilityAndGrouping().setSourceActive( gridSourceIndex, false );
		
		
		/* Set up LandmarkTableModel, holds the data and 
		 * interfaces with the LandmarkPanel */
		landmarkModel = new LandmarkTableModel( ndims );
		landmarkModellistener = new LandmarkTableListener();
		landmarkModel.addTableModelListener(landmarkModellistener);
		landmarkTable = new JTable(landmarkModel);
		
		overlayP = new BigWarpOverlay( viewerP, landmarkModel );
		overlayQ = new BigWarpOverlay( viewerQ, landmarkModel );
		viewerP.addOverlay( overlayP );
		viewerQ.addOverlay( overlayQ );
		
		dragOverlayP = new BigWarpDragOverlay( this, viewerP );
		dragOverlayQ = new BigWarpDragOverlay( this, viewerQ );
		viewerP.addDragOverlay( dragOverlayP );
		viewerQ.addDragOverlay( dragOverlayQ );
		
		/* Set up landmark panel */
		landmarkPanel = new BigWarpLandmarkPanel( landmarkModel );
		landmarkPanel.setOpaque( true );
		addDefaultTableMouseListener();
		
		landmarkFrame = new BigWarpLandmarkFrame( "Landmarks", landmarkPanel, this );
		
		landmarkPopupMenu = new LandmarkPointMenu( this );
		landmarkPopupMenu.setupListeners( );
		
		ArrayList<ConverterSetup> csetups = new ArrayList<ConverterSetup>();
		for ( final ConverterSetup cs : converterSetups )
		{
			csetups.add( new BigWarpConverterSetupWrapper( this, cs) );
		}
		
		setupAssignments = new SetupAssignments( csetups, 0, 512 );
		if ( setupAssignments.getMinMaxGroups().size() > 0 )
		{
			final MinMaxGroup group = setupAssignments.getMinMaxGroups().get( 0 );
			for ( final ConverterSetup setup : setupAssignments.getConverterSetups() )
				setupAssignments.moveSetupToGroup( setup, group );
		}
		
		brightnessDialog = new BrightnessDialog( getViewerFrameQ(), setupAssignments );
		helpDialog = new HelpDialog( getViewerFrameP() );
		
		warpVisDialog = new WarpVisFrame( viewerFrameQ, this ); // dialogs have to be constructed before action maps are made
		
		final KeyProperties keyProperties = KeyProperties.readPropertyFile();
		WarpNavigationActions.installActionBindings( getViewerFrameP().getKeybindings(), viewerP, keyProperties, (ndims==2) );
		BigWarpActions.installActionBindings( getViewerFrameP().getKeybindings(), this, keyProperties);
		
		WarpNavigationActions.installActionBindings( getViewerFrameQ().getKeybindings(), viewerQ, keyProperties, (ndims==2) );
		BigWarpActions.installActionBindings( getViewerFrameQ().getKeybindings(), this, keyProperties);
		
		BigWarpActions.installLandmarkPanelActionBindings( landmarkFrame.getKeybindings(), this, landmarkTable, keyProperties );
		
		// this call has to come after the actions are set
		warpVisDialog.setActions();
		
		setUpViewerMenu( viewerFrameP );
		setUpViewerMenu( viewerFrameQ );
		setUpLandmarkMenus();
		
		
		/* Set the locations of frames */
		Point viewerFramePloc = getViewerFrameP().getLocation();
		viewerFramePloc.setLocation( viewerFramePloc.x + DEFAULT_WIDTH, viewerFramePloc.y );
		getViewerFrameQ().setLocation( viewerFramePloc );
		viewerFramePloc.setLocation( viewerFramePloc.x + DEFAULT_WIDTH, viewerFramePloc.y );
		landmarkFrame.setLocation( viewerFramePloc );
		
		landmarkClickListenerP = new MouseLandmarkListener( this.viewerP );
		landmarkClickListenerQ = new MouseLandmarkListener( this.viewerQ );
		
		// have to be safe here and use 3dim point for both 3d and 2d
		currentLandmark = new RealPoint( 3 ); 
		inLandmarkMode = false;
		setupKeyListener( );
		
		InitializeViewerState.initTransform( viewerP );
		InitializeViewerState.initTransform( viewerQ );
		
		viewerFrameP.setVisible(true);
		viewerFrameQ.setVisible(true);
		landmarkFrame.setVisible(true);
		
		initialViewP = new AffineTransform3D();
		initialViewQ = new AffineTransform3D();
		viewerP.getState().getViewerTransform( initialViewP );
		viewerQ.getState().getViewerTransform( initialViewQ );
		
		checkBoxInputMaps();
		
		// file selection
		fileFrame = new JFrame("Select File");
		fileDialog = new FileDialog(fileFrame);
		lastDirectory = null;
		
		fileFrame.setVisible( false );
	}
	
	public void closeAll()
	{
		viewerFrameP.setVisible( false );
		viewerFrameQ.setVisible( false );
		landmarkFrame.setVisible( false );
		
		viewerFrameP.getViewerPanel().stop();
		viewerFrameQ.getViewerPanel().stop();
		
		viewerFrameP.dispose();
		viewerFrameQ.dispose();
		landmarkFrame.dispose();
	}
	
	public void invertPointCorrespondences()
	{
		System.out.println( "inverting point correspondences" );
		landmarkModel = landmarkModel.invert();
		landmarkPanel.setTableModel( landmarkModel );
	}
	
	public File getLastDirectory()
	{
		return lastDirectory;
	}
	
	public void setLastDirectory( File dir )
	{
		this.lastDirectory = dir;
	}
	
	public void setImageJInstance( ImageJ ij )
	{
		BigWarp.ij = ij;
		
		if( BigWarp.ij != null )
			setupImageJExportOption();
	}
	
	protected void setUpViewerMenu( BigWarpViewerFrame vframe )
	{
		//TODO setupviewermenu
		
		final ActionMap actionMap = vframe.getKeybindings().getConcatenatedActionMap();
		
		JMenuBar viewerMenuBar = new JMenuBar();
		JMenu settingsMenu    = new JMenu( "Settings" );
		viewerMenuBar.add( settingsMenu );
		
		final JMenuItem miBrightness = new JMenuItem( actionMap.get( BigWarpActions.BRIGHTNESS_SETTINGS ) );
		miBrightness.setText( "Brightness & Color" );
		settingsMenu.add( miBrightness );
		
		/* Warp Visualization */
		final JMenuItem warpVisMenu = new JMenuItem( actionMap.get( BigWarpActions.SHOW_WARPTYPE_DIALOG ) );
		warpVisMenu.setText( "Warp Visualization" );
		settingsMenu.add( warpVisMenu );
		
		vframe.setJMenuBar( viewerMenuBar );
		
		JMenu helpMenu    = new JMenu( "Help" );
		viewerMenuBar.add( helpMenu );
		
		final JMenuItem miHelp = new JMenuItem( actionMap.get( BigWarpActions.SHOW_HELP ) );
		miHelp.setText( "Show Help Menu" );
		helpMenu.add( miHelp );
	}
	
	protected void setupImageJExportOption()
	{
		final JMenuItem exportToVImagePlus = new JMenuItem("Export as Virtual ImagePlus");
        landmarkMenu.add( exportToVImagePlus );
        exportToVImagePlus.addMouseListener( new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) 
			{
				new Thread() {
					public void run() 
					{
						try 
						{
							exportMovingImagePlus( true );
						}
						catch( Exception e ){ e.printStackTrace(); }
					}
				}.start();
			}
        });
        
        final JMenuItem exportToImagePlus = new JMenuItem("Export as ImagePlus");
        landmarkMenu.add( exportToImagePlus );
        exportToImagePlus.addMouseListener( new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) 
			{
				new Thread() {
					public void run() 
					{
						try 
						{
							exportMovingImagePlus( false );
						}
						catch( Exception e ){ e.printStackTrace(); }
					}
				}.start();
			}
        });
        
	}
	
	protected void setUpLandmarkMenus()
	{
		JMenuBar landmarkMenuBar = new JMenuBar();
        
		landmarkMenu    = new JMenu( "File" );
        final JMenuItem openItem = new JMenuItem("Import landmarks");
        landmarkMenu.add( openItem );
        
        final JMenuItem saveItem = new JMenuItem("Export landmarks");
        landmarkMenu.add( saveItem );

        landmarkMenu.addSeparator();
        final JMenuItem exportImageItem = new JMenuItem("Export Moving Image");
        landmarkMenu.add( exportImageItem );
        
//        landmarkMenu.addSeparator();
//        final JMenuItem importXfmItem = new JMenuItem("Import Transformation");
//        landmarkMenu.add( importXfmItem );
        
//        final JMenuItem exportXfmItem = new JMenuItem("Export Transformation");
//        landmarkMenu.add( exportXfmItem );
        
        landmarkMenuBar.add( landmarkMenu );
        landmarkFrame.setJMenuBar( landmarkMenuBar );
        
        openItem.addMouseListener( new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) 
			{
				//System.out.println( "getLastDirectory is: " + getLastDirectory());
				final JFileChooser fc = new JFileChooser( getLastDirectory() );
				
				fc.showOpenDialog( landmarkFrame );
				File file = fc.getSelectedFile();
				
				setLastDirectory( file.getParentFile() );
				//System.out.println( "now is: " + getLastDirectory());
				
				//fileFrame.setVisible( true );
//				fileDialog.setVisible( true );
//				File file = new File( fileDialog.getFile() );
				//int returnval = fc.showOpenDialog( landmarkFrame );
				
				//System.out.println("to open file: " + file );
				try 
				{
					landmarkModel.load( file );
				} 
				catch (IOException e1) 
				{
					e1.printStackTrace();
				}
				
				//landmarkTable.repaint();
				landmarkFrame.repaint();
				
			}
        });
        
        saveItem.addMouseListener( new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {
				//System.out.println( "clicked save ");
			}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) 
			{
				//System.out.println( "getLastDirectory is: " + getLastDirectory());
				final JFileChooser fc = new JFileChooser( getLastDirectory() );
				// int returnval = fc.showOpenDialog( landmarkFrame );
				fc.showSaveDialog( landmarkFrame );
				File file = fc.getSelectedFile();
				
				setLastDirectory( file.getParentFile() );
				//System.out.println( "now is: " + getLastDirectory());
				
				if( file == null )
					return;
				
				//System.out.println("to save file: " + file );
				try 
				{
					landmarkModel.save( file );
				} 
				catch (IOException e1) 
				{
					e1.printStackTrace();
				}
				
			}
        });
        
        exportImageItem.addMouseListener( new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseReleased(MouseEvent e) 
			{
				//System.out.println( "getLastDirectory is: " + getLastDirectory());
				final JFileChooser fc = new JFileChooser( getLastDirectory() );
				//int returnval = fc.showOpenDialog( landmarkFrame );
				fc.showSaveDialog( landmarkFrame );
				
				final ViewerState state = viewerP.getState().copy();
				final File file = fc.getSelectedFile();
				
				setLastDirectory( file.getParentFile() );
				//System.out.println( "now is: " + getLastDirectory());
				
				
				if( file == null )
					return;
				
				//TODO exportThread
				new Thread() {
					public void run() {
						try 
						{
							exportMovingImage( file, state, progressWriter );
						}
						catch( Exception e1 )
						{
							e1.printStackTrace();
						}
					}
				}.start();
			}
        });
	
//        exportXfmItem.addMouseListener( new MouseListener(){
//
//			@Override
//			public void mouseClicked(MouseEvent arg0) {}
//			@Override
//			public void mouseEntered(MouseEvent arg0) {}
//			@Override
//			public void mouseExited(MouseEvent arg0) {}
//			@Override
//			public void mousePressed(MouseEvent arg0) {}
//
//			@Override
//			public void mouseReleased(MouseEvent arg0) 
//			{
//				final JFileChooser fc = new JFileChooser();
//				int returnval = fc.showOpenDialog( landmarkFrame );
//				File file = fc.getSelectedFile();
//				FileUtils.
//				try 
//				{
//					exportTransformation( file,  );
//				}
//				catch( Exception e1 )
//				{
//					e1.printStackTrace();
//				}
//			}
//        });
	}
	
	public BigWarpViewerFrame getViewerFrameP() {
		return viewerFrameP;
	}

	public BigWarpViewerFrame getViewerFrameQ() {
		return viewerFrameQ;
	}
	
	public BigWarpOverlay getOverlayP()
	{
		return overlayP;
	}
	
	public BigWarpOverlay getOverlayQ()
	{
		return overlayQ;
	}
	
	public SetupAssignments getSetupAssignments()
	{
		return setupAssignments;
	}
	
	public ArrayList<SourceAndConverter<?>> getSources()
	{
		return sources;
	}

	public BigWarpLandmarkPanel getLandmarkPanel()
	{
		return landmarkPanel;
	}
	
	public boolean isInLandmarkMode(){
		return inLandmarkMode;
	}

	public void setInLandmarkMode( boolean inLmMode )
	{
		if( inLmMode )
		{
			disableTransformHandlers();
			viewerP.showMessage("Landmark mode on ( Moving image )");
			viewerQ.showMessage("Landmark mode on ( Fixed image )");
			viewerFrameP.setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
			viewerFrameQ.setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
		}
		else
		{
			enableTransformHandlers();
			viewerP.showMessage("Landmark mode off ( Moving image )");
			viewerQ.showMessage("Landmark mode off ( Fixed image )");
			viewerFrameP.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
			viewerFrameQ.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
		}
		
		inLandmarkMode = inLmMode;
	}
	
	/**
	 * 
	 * @param ptarray
	 * @param isMoving
	 * @param selectedPointIndex
	 * @param viewer
	 */
	public void updatePointLocation( double[] ptarray, boolean isMoving, int selectedPointIndex, BigWarpViewerPanel viewer )
	{
		boolean isMovingViewerXfm = viewer.getOverlay().getIsTransformed();
		
		// TODO avoid duplicate effort and comment this section 
		
		
		if( landmarkModel.getTransform() == null || !isMovingViewerXfm )
		{
			landmarkModel.setPoint( selectedPointIndex, isMoving, ptarray, false );
			
			if( !isMoving && !landmarkModel.isWarpedPositionChanged( selectedPointIndex ))
				landmarkModel.updateWarpedPoint( selectedPointIndex, ptarray );
		}
		else
		{
			//System.out.println("HERE");
			landmarkModel.getTransform().apply( ptarray, ptBack);
			landmarkModel.updateWarpedPoint( selectedPointIndex, ptarray );
			
			//BigWarp.this.landmarkModel.setPoint( selectedPointIndex, isMoving, ptBack );
		}
		
//		if( isMoving && landmarkModel.getTransform()!= null && isMovingViewerXfm )
//		{
//			System.out.println("HERE1");
//			landmarkModel.getTransform().apply( ptarray, ptBack);
//			landmarkModel.updateWarpedPoint( selectedPointIndex, ptarray );
//		}
//		
//		if( landmarkModel.getTransform() == null || !isMovingViewerXfm )
//		{
//			landmarkModel.setPoint( selectedPointIndex, isMoving, ptarray, false );
//			
//			if( !isMoving && !landmarkModel.isWarpedPositionChanged( selectedPointIndex ))
//				landmarkModel.updateWarpedPoint( selectedPointIndex, ptarray );
//		}
//		else
//		{
//			System.out.println("HERE2");
//			//BigWarp.this.landmarkModel.setPoint( selectedPointIndex, isMoving, ptBack );
//		}
		
		if( landmarkFrame.isVisible() )
		{
			landmarkFrame.repaint();
			viewer.requestRepaint();
		}
	}
	
	public void updatePointLocation( double[] ptarray, boolean isMoving, int selectedPointIndex )
	{
		if( isMoving )
			updatePointLocation( ptarray, isMoving, selectedPointIndex, viewerP );
		else
			updatePointLocation( ptarray, isMoving, selectedPointIndex, viewerQ );
	}
	
	public int updateWarpedPoint( double[] ptarray, boolean isMoving )
	{
		int selectedPointIndex = selectedLandmark( ptarray, isMoving );
		
		// if a fixed point is changing its location, 
		// we need to update the warped position for the corresponding moving point
		// so that it can be rendered correctly
		if( selectedPointIndex >= 0 && !isMoving && landmarkModel.getTransform() != null )
		{
			landmarkModel.updateWarpedPoint( selectedPointIndex, ptarray );
		}
		
		return selectedPointIndex;
	}
	
	/**
	 * Updates the global variable ptBack
	 * @param ptarray
	 * @param isMoving
	 */
	public String addPoint( double[] ptarray, boolean isMoving, BigWarpViewerPanel viewer )
	{
		boolean isViewerTransformed = viewer.getOverlay().getIsTransformed();

		if( !isMoving && viewer.getIsMoving() && !isViewerTransformed )
			return "Adding a fixed point in moving image space not supported";
		else
			return addPoint( ptarray, isMoving );
	}
	
	/**
	 * Updates the global variable ptBack
	 * @param ptarray
	 * @param isMoving
	 */
	public String addPoint( double[] ptarray, boolean isMoving )
	{
//		System.out.println("isMoving: " + isMoving );
//		System.out.println("landmarkModel.getTransform(): " + landmarkModel.getTransform() );
//		System.out.println("BigWarp.this.isMovingDisplayTransformed() : " + BigWarp.this.isMovingDisplayTransformed()  );
		
		String message = "";
		// We need to transform the point if:
		// Adding a moving point in a viewer in target-image-space
		// could be a transformed viewer panel or
		// the fixed image viewer panel
		//
		// Adding a fixed point in the space of the moving image requires
		// the inverse transform and is currently not allowed
		//
		// In all other cases, the raw point can be added directly
		if( isMoving && landmarkModel.getTransform() != null && BigWarp.this.isMovingDisplayTransformed() )
		{
			//System.out.println("Add point moving, warped space");
			landmarkModel.getTransform().apply( ptarray, ptBack );
			
			BigWarp.this.landmarkModel.add( ptBack, isMoving );
			
			int i = (BigWarp.this.landmarkModel.getRowCount() - 1);
			//System.out.println(" new index: " + i );
			
			landmarkModel.updateWarpedPoint( i, ptarray );
		}
		else
			BigWarp.this.landmarkModel.add( ptarray, isMoving );
		
		if( BigWarp.this.landmarkFrame.isVisible() ){
			BigWarp.this.landmarkFrame.repaint();
		}
		return message;
	}
	
	/**
	 * Updates the global variable ptBack
	 * @param ptarray
	 * @param isMoving
	 * @return
	 */
	public int selectedLandmark( double[] ptarray, boolean isMoving )
	{
		int selectedPointIndex = -1;
		if( isMoving && landmarkModel.getTransform() != null && isMovingDisplayTransformed() )
		{
			
			landmarkModel.getTransform().apply( ptarray, ptBack );
			//System.out.println("ptBack: " + XfmUtils.printArray( ptBack ));
			selectedPointIndex = selectedLandmarkHelper( ptBack, isMoving );
		}
		else // check if the clicked point itself is in the table 
		{
			//System.out.println("ptarray: " + XfmUtils.printArray( ptarray ));
			selectedPointIndex = selectedLandmarkHelper( ptarray, isMoving );
		}
		return selectedPointIndex;
	}
	
	protected int selectedLandmarkHelper( double[] pt, boolean isMoving )
	{
		// TODO selectedLandmark
		int N = landmarkModel.getRowCount();
		
		double radsq = 100;
		AffineTransform3D viewerXfm = new AffineTransform3D(); 
		if( isMoving )
		{
			viewerP.getState().getViewerTransform( viewerXfm );
			radsq = viewerP.getSettings().getSpotSize();
		}
		else
		{
			viewerQ.getState().getViewerTransform( viewerXfm );
			radsq = viewerQ.getSettings().getSpotSize();
		}
		radsq = (radsq*radsq);
		double scale = computeScaleAssumeRigid( viewerXfm );
		
		
		
		double dist = 0.0;
		int bestIdx = -1;
		double smallestDist = Double.MAX_VALUE;
		
		for( int n = 0; n < N; n++ )
		{
			Double[] lmpt = landmarkModel.getPoints( isMoving ).get(n);
			
			dist = 0.0;
			for( int i = 0; i < ndims; i++ )
			{
				dist += (pt[i] - lmpt[i]) * (pt[i] - lmpt[i]);
			}
			
			dist *= (scale*scale);
			if( dist < radsq && dist < smallestDist )
			{
				smallestDist = dist;
				bestIdx = n;
			}
		}
		
		if( landmarkFrame.isVisible() )
		{
			landmarkTable.setEditingRow( bestIdx );
			landmarkFrame.repaint();
		}
		
		return bestIdx;
	}
	
	public static double computeScaleAssumeRigid( AffineTransform3D xfm )
	{
		return xfm.get( 0, 0 ) + xfm.get( 0, 1 ) + xfm.get( 0, 2 );
	}
	
	public void toggleInLandmarkMode()
	{
		setInLandmarkMode( !inLandmarkMode );
	}
	
	protected void disableTransformHandlers()
	{
		// disable navigation listeners
		handlerP = viewerP.getDisplay().getTransformEventHandler();
		viewerP.getDisplay().setTransformEventHandler( DUMMY_TRANSFORM_HANDLER );
		
		viewerP.setTransformEnabled( false );
		viewerQ.setTransformEnabled( false );
		
		handlerQ = viewerQ.getDisplay().getTransformEventHandler();
		viewerQ.getDisplay().setTransformEventHandler( DUMMY_TRANSFORM_HANDLER );
	}
	
	protected void enableTransformHandlers()
	{
		// enable navigation listeners
		viewerP.setTransformEnabled( true );
		viewerQ.setTransformEnabled( true );
		
		if( handlerP != null )
			viewerP.getDisplay().setTransformEventHandler( handlerP );
		
		if( handlerQ != null )
			viewerQ.getDisplay().setTransformEventHandler( handlerQ );
	}
	
	/**
	 * Changes the view transformation of 'panelToChange' to match that of 'panelToMatch'
	 * @param panelToChange
	 * @param panelToMatch
	 */
	protected void matchWindowTransforms( BigWarpViewerPanel panelToChange, BigWarpViewerPanel panelToMatch, AffineTransform3D toPreconcat )
	{
		panelToChange.showMessage( "Aligning" );
		panelToMatch.showMessage( "Matching alignment" );
		
		// get the transform from panelToMatch
		AffineTransform3D viewXfm = new AffineTransform3D();
		panelToMatch.getState().getViewerTransform( viewXfm );
		
		// change transform of panelToChange
		panelToChange.animateTransformation( viewXfm );
		
		AffineTransform3D resXfm = new AffineTransform3D();
		panelToChange.getState().getViewerTransform( resXfm );
		
	}
	
	public void matchOtherViewerPanelToActive()
	{
		BigWarpViewerPanel panelToChange;
		BigWarpViewerPanel panelToMatch;
		
		AffineTransform3D toPreconcat = null;
		
		if( viewerFrameP.isActive() )
		{
			panelToChange = viewerQ;
			panelToMatch  = viewerP;
		}
		else if( viewerFrameQ.isActive() )
		{
			panelToChange = viewerP;
			panelToMatch  = viewerQ;
			toPreconcat = fixedViewXfm;
		}
		else
			return;
		
		matchWindowTransforms( panelToChange, panelToMatch, toPreconcat );
	}
	
	public void matchActiveViewerPanelToOther()
	{
		BigWarpViewerPanel panelToChange;
		BigWarpViewerPanel panelToMatch;
		
		AffineTransform3D toPreconcat = null;
		
		if( viewerFrameP.isActive() )
		{
			panelToChange = viewerP;
			panelToMatch  = viewerQ;
			toPreconcat = fixedViewXfm;
		}
		else if( viewerFrameQ.isActive() )
		{
			panelToChange = viewerQ;
			panelToMatch  = viewerP;
		}
		else
			return;
		
		matchWindowTransforms( panelToChange, panelToMatch, toPreconcat );
	}
	
	public void resetView()
	{
		final RandomAccessibleInterval<?> interval = getSources().get( 1 ).getSpimSource().getSource( 0, 0 );
		
		AffineTransform3D viewXfm = new AffineTransform3D();
		viewXfm.identity();
		viewXfm.set( -interval.min( 2 ), 2, 3 );
		
		if( viewerFrameP.isActive() )
		{
			if( viewerP.getOverlay().getIsTransformed() )
				viewerP.animateTransformation( initialViewQ );
			else
				viewerP.animateTransformation( initialViewP );
		}
		else if ( viewerFrameQ.isActive() )
			viewerQ.animateTransformation( initialViewQ );
	}
	
	public void toggleNameVisibility()
	{
		viewerSettings.toggleNamesVisible();
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}
	
	public void togglePointVisibility()
	{
		viewerSettings.togglePointsVisible();
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}
	
	/**
	 * Toggles whether the moving image is displayed after warping
	 * (in the same space as the fixed image), or in its native space. 
	 */
	public void toggleMovingImageDisplay()
	{
		boolean newState =  !getOverlayP().getIsTransformed();
		
		if( newState )
			getViewerFrameP().getViewerPanel().showMessage("Displaying warped");
		else
			getViewerFrameP().getViewerPanel().showMessage("Displaying raw");
		
		// Toggle whether moving image is displayed as transformed or not
		setIsMovingDisplayTransformed( newState );
		getViewerFrameP().getViewerPanel().requestRepaint();
		
		if( viewerQ.getVisibilityAndGrouping().isFusedEnabled())
		{
			getViewerFrameQ().getViewerPanel().requestRepaint();
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void exportMovingImagePlus( boolean isVirtual )
	{
		//TODO exportMovingImagePlus
		if( ij == null )
		{
			System.out.println("no imagej instance");
			return;
		}
		
		final RandomAccessibleInterval<?> destinterval = sources.get( fixedSourceIndex ).getSpimSource().getSource( 0, 0 );
		AffineTransform3D fixedImgXfm = sources.get( fixedSourceIndex ).getSpimSource().getSourceTransform( 0, 0 );
		
		Interpolation interp = viewerP.getState().getInterpolation();
		RealRandomAccessible< ? > raiRaw = sources.get( movingSourceIndex ).getSpimSource().getInterpolatedSource( 0, 0, interp );
		AffineTransform3D xfmInv = fixedImgXfm.inverse();
		
		AffineRandomAccessible< ?, AffineGet > rai = RealViews.affine( raiRaw, xfmInv );
		
		ImagePlus ip = null;
		Object t = rai.realRandomAccess().get();
		
		if( isVirtual )
		{
			// return a wrapper
			IntervalView rasterItvl = Views.interval( Views.raster( rai ), destinterval );
			ip = ImageJFunctions.wrap( rasterItvl, "warped_moving_image" );
		}
		else
		{
			// This is annoying ... is there a better way?
			if ( ByteType.class.isInstance( movingSourceType ) )
				ip = copyToImageStack( (RealRandomAccessible< ByteType > )rai, destinterval );
			else if ( UnsignedByteType.class.isInstance( movingSourceType ) )
				ip = copyToImageStack( (RealRandomAccessible< UnsignedByteType > )rai, destinterval );
			else if ( IntType.class.isInstance( movingSourceType ) )
				ip = copyToImageStack( (RealRandomAccessible< IntType > )rai, destinterval );
			else if ( FloatType.class.isInstance( movingSourceType ) )
				ip = copyToImageStack( (RealRandomAccessible< FloatType > )rai, destinterval );
			else if ( DoubleType.class.isInstance( movingSourceType ) )
				ip = copyToImageStack( (RealRandomAccessible< DoubleType > )rai, destinterval );
			else if ( ARGBType.class.isInstance( movingSourceType ) )
				ip = copyToImageStack( (RealRandomAccessible< ARGBType > )rai, destinterval );
			else
			{
				System.err.println("Can't convert type " + movingSourceType.getClass() + 
				 	" to ImagePlus" );
				return;
			}
		}
		
		if( ip != null )
			ip.show();
		
	}
	
	public static < T extends NumericType< T > & NativeType< T > > ImagePlus copyToImageStack( RealRandomAccessible<T> rai, Interval itvl )
	{
		System.out.println("copy to stack");
		long[] dimensions = new long[ itvl.numDimensions() ];
		itvl.dimensions( dimensions );
		
		T t = rai.realRandomAccess().get();
		ImagePlusImgFactory<T> factory = new ImagePlusImgFactory<T>();
		ImagePlusImg< T, ? > target = factory.create( itvl, t );
		
		double k = 0;
		long N = dimensions[0] * dimensions[1] * dimensions[2];
		
		net.imglib2.Cursor< T > c = target.cursor();
		RealRandomAccess< T > ra = rai.realRandomAccess();
		while( c.hasNext() )
		{
			c.fwd();
			ra.setPosition( c );
			c.get().set( ra.get() );
			
			if( k % 10000 == 0 ){
				//System.out.println(" progress: " +  (k/N));
				IJ.showProgress( k / N );
			}
			k++;
		}
		
		IJ.showProgress( 1.1 );
		try {
			return target.getImagePlus();
		} catch (ImgLibException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	protected void exportMovingImage( final File f, final ViewerState renderState, ProgressWriter progressWriter ) throws IOException, InterruptedException
	{
		// Source< ? > movingSrc = sources.get( 1 ).getSpimSource();
		final RandomAccessibleInterval<?> interval = sources.get( 1 ).getSpimSource().getSource( 0, 0 );
		
		// stolen from bdv.tools.RecordMovieDialog
		class MyTarget implements RenderTarget
		{
			BufferedImage bi;

			@Override
			public BufferedImage setBufferedImage( final BufferedImage bufferedImage )
			{
				bi = bufferedImage;
				return null;
			}

			@Override
			public int getWidth()
			{
				return (int)(interval.max( 0 ) - interval.min( 0 ) + 1);
			}

			@Override
			public int getHeight()
			{
				return (int)(interval.max( 1 ) - interval.min( 1 ) + 1);
			}
		}
		
//		final ViewerState renderState = viewerP.getState().copy();
		
		int minz = (int)interval.min( 2 );
		int maxz = (int)interval.max( 2 );
		
//		if( ndims == 2 && maxz == 0 )
//			maxz = 0;
		
		final AffineTransform3D viewXfm = new AffineTransform3D();
		viewXfm.identity();
		
		final MyTarget target = new MyTarget();
		int numRenderThreads = 1;		
		
		final MultiResolutionRenderer renderer = new MultiResolutionRenderer( target, new PainterThread( null ), 
				new double[] { 1 }, 0, false, numRenderThreads, null, false, 
				viewerP.getOptionValues().getAccumulateProjectorFactory(), new Cache.Dummy() );
		
		progressWriter.setProgress( 0 );
		
		// step through z, rendering each slice as an image and writing it to 
		for( int z = minz; z <= maxz; z++ )
		{	
			viewXfm.set( -z, 2, 3 );
			renderState.setViewerTransform( viewXfm );
			renderer.requestRepaint();
			renderer.paint( renderState );

			File thiszFile = new File( String.format( "%s_z-%04d.png", f.getAbsolutePath(), z ) );
			progressWriter.setProgress( ( double ) ( z - minz + 1) / ( maxz - minz + 1) );
			
			ImageIO.write( target.bi, "png", thiszFile );
		}
	}
	
	protected void addDefaultTableMouseListener()
	{
		landmarkTableListener = new MouseLandmarkTableListener( );
		landmarkPanel.getJTable().addMouseListener( landmarkTableListener );
	}
	
	public void setGridType( GridSource.GRID_TYPE method )
	{
		(( GridSource<?> ) sources.get(  gridSourceIndex ).getSpimSource() ).setMethod( method );
	}
	
	private static ArrayList< SourceAndConverter< ? >> wrapSourcesAsTransformed( ArrayList< SourceAndConverter< ? > > sources, int ndims, int warpUsIndices )
	{
		ArrayList< SourceAndConverter< ? >> wrappedSource = new ArrayList< SourceAndConverter< ? >>();
		
		int i = 0;
		for( SourceAndConverter< ? > sac : sources )
		{
			if( i == warpUsIndices )
			{
				wrappedSource.add( wrapSourceAsTransformed( sac, "xfm_" + i, ndims  ));
			}
			else
			{
				wrappedSource.add( sac );
			}
			
			i++;
		}
		return wrappedSource;
	}
	
	/**
	 * This is the sahnehaubchen :-)
	 * @param sources
	 * @param name
	 * @param data
	 * @return the index into sources where this source was added
	 */
	private static int addWarpMagnitudeSource( ArrayList< SourceAndConverter< ? > > sources, ArrayList<ConverterSetup> converterSetups, String name, BigWarpData data )
	{
		// TODO think about whether its worth it to pass a type parameter.
		// or should we just stick with Doubles?
		
		WarpMagnitudeSource< FloatType > magSource = new WarpMagnitudeSource< FloatType >( name, data, new FloatType() );
		
		final RealARGBColorConverter< VolatileFloatType > vconverter = new RealARGBColorConverter.Imp0< VolatileFloatType >( 0, 512 );
		vconverter.setColor( new ARGBType( 0xffffffff ) );
		final RealARGBColorConverter< FloatType > converter = new RealARGBColorConverter.Imp1< FloatType >( 0, 512 );
		converter.setColor( new ARGBType( 0xffffffff ) );
		
		int id = (int)(Math.random() * Integer.MAX_VALUE );
		converterSetups.add( new RealARGBColorConverterSetup( id, converter, vconverter ) );
		
		final SourceAndConverter< FloatType > soc = new SourceAndConverter< FloatType >( magSource, converter, null );
		sources.add( soc );
		
		return sources.size() - 1;
	}
	
	/**
	 * This is the sahnehaubchen :-)
	 * @param sources
	 * @param name
	 * @param data
	 * @return the index into sources where this source was added
	 */
	private static int addGridSource( ArrayList< SourceAndConverter< ? > > sources, ArrayList<ConverterSetup> converterSetups, String name, BigWarpData data )
	{
		// TODO think about whether its worth it to pass a type parameter.
		// or should we just stick with Floats?
		
		GridSource< FloatType > magSource = new GridSource< FloatType >( name, data, new FloatType(), null );
		
		final RealARGBColorConverter< VolatileFloatType > vconverter = new RealARGBColorConverter.Imp0< VolatileFloatType >( 0, 512 );
		vconverter.setColor( new ARGBType( 0xffffffff ) );
		final RealARGBColorConverter< FloatType > converter = new RealARGBColorConverter.Imp1< FloatType >( 0, 512 );
		converter.setColor( new ARGBType( 0xffffffff ) );
		
		int id = (int)(Math.random() * Integer.MAX_VALUE );
		converterSetups.add( new RealARGBColorConverterSetup( id, converter, vconverter ) );
		
		final SourceAndConverter< FloatType > soc = new SourceAndConverter< FloatType >( magSource, converter, null );
		sources.add( soc );
		
		return sources.size() - 1;
	}
	
	private static < T > SourceAndConverter< T > wrapSourceAsTransformed( SourceAndConverter< T > src, String name, int ndims )
	{
		if( src.asVolatile() == null )
		{
			return new SourceAndConverter<T>( 
					new WarpedSource<T>( src.getSpimSource(), name ),
					src.getConverter(), null );
		}
		else
		{
			return new SourceAndConverter<T>( 
				new WarpedSource<T>( src.getSpimSource(), name ),
				src.getConverter(), wrapSourceAsTransformed( src.asVolatile(), name + "_vol", ndims ) );
		}
	}
		 
	public static SpimDataMinimal loadSpimData( String xmlPath ){
		SpimDataMinimal spimData = null;
		try {
			spimData = new XmlIoSpimDataMinimal().load( xmlPath );
			
			if ( WrapBasicImgLoader.wrapImgLoaderIfNecessary( spimData ) )
			{
				System.err.println( "WARNING:\nOpening <SpimData> dataset that is not suited for interactive browsing.\nConsider resaving as HDF5 for better performance." );
			}
			
		} catch (SpimDataException e) 
		{
			e.printStackTrace();
		}

		return spimData;
	}
	
	public void setupKeyListener( )
	{
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor( 
				new LandmarkKeyboardProcessor( this ));
	}
	
	public void setWarpVisGridType( GridSource.GRID_TYPE type )
	{
		((GridSource<?>)sources.get( gridSourceIndex ).getSpimSource()).setMethod( type );
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}
	
	public void setWarpGridWidth( double width )
	{
		((GridSource<?>)sources.get( gridSourceIndex ).getSpimSource()).setGridWidth( width );
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}
	
	public void setWarpGridSpacing( double spacing )
	{
		((GridSource<?>)sources.get( gridSourceIndex ).getSpimSource()).setGridSpacing( spacing );
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}
	
	public void setWarpMagBaseline( int i )
	{
		AbstractModel<?> baseline = this.baseXfmList[ i ];
		((WarpMagnitudeSource<?>) sources.get( warpMagSourceIndex ).getSpimSource()).setBaseline( baseline );
		
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}
	
	protected void setupWarpMagBaselineOptions( CoordinateTransform[] xfm, int ndim )
	{
		if( ndim == 2 )
		{
			xfm[ 0 ] = new AffineModel2D();
			xfm[ 1 ] = new SimilarityModel2D();
			xfm[ 2 ] = new RigidModel2D();
		}
		else
		{
			xfm[ 0 ] = new AffineModel3D();
			xfm[ 1 ] = new SimilarityModel3D();
			xfm[ 2 ] = new RigidModel3D();
		}
	}
	
	protected void fitBaselineWarpMagModel()
	{
		double[][] p = new double[ ndims ][ landmarkModel.getTransform().getNumActiveLandmarks() ];
		double[][] q = new double[ ndims ][ landmarkModel.getTransform().getNumActiveLandmarks() ];
		double[] w   = new double[ landmarkModel.getTransform().getNumLandmarks() ];
		
		int k = 0;
		for( int i = 0; i < landmarkModel.getTransform().getNumLandmarks(); i++ )
		{
			if( landmarkModel.getTransform().isActive( i ))
			{
				w[ k ] = 1.0;
				
				for( int d = 0; d < ndims; d++ )
				{
					p[ d ][ k ] = landmarkModel.getTransform().getSourceLandmarks()[ d ][ i ];
					q[ d ][ k ] = landmarkModel.getTransform().getTargetLandmarks()[ d ][ i ];
				}
				k++;
			}
		}
		
		try 
		{
			((WarpMagnitudeSource<?>) sources.get( warpMagSourceIndex ).getSpimSource()).getBaseline().fit( p, q, w );
		} 
		catch (NotEnoughDataPointsException e) 
		{
			e.printStackTrace();
		} 
		catch (IllDefinedDataPointsException e) 
		{
			e.printStackTrace();
		}
		
	}
	
	public enum WarpVisType { NONE, WARPMAG, GRID };
	
	public void setWarpVisMode( WarpVisType type, BigWarpViewerFrame viewerFrame, boolean both )
	{
		if( viewerFrame == null )
		{
			if( viewerFrameP.isActive() )
			{
				viewerFrame = viewerFrameP;
			}
			else if( viewerFrameQ.isActive() )
			{
				viewerFrame = viewerFrameQ;
			}else if( both ){
				setWarpVisMode( type, viewerFrameP, false );
				setWarpVisMode( type, viewerFrameQ, false );
				return;
			}else{
				return;
			}
				
		}
		
		int offImgIndex = 0;
		int onImgIndex  = 1;
		
		if( viewerFrame == viewerFrameP )
		{
			offImgIndex = 1;
			onImgIndex  = 0;
		}
		
		if( landmarkModel.getTransform() == null )
		{
			viewerFrame.getViewerPanel().showMessage( "No warp - estimate warp first." );
			return;
		}
		VisibilityAndGrouping vg = viewerFrame.getViewerPanel().getVisibilityAndGrouping();
		
		switch( type ) {
		case WARPMAG: 
		{
			// turn warp mag on
			vg.setSourceActive( warpMagSourceIndex, true );
			vg.setSourceActive( gridSourceIndex, false);
			vg.setSourceActive( offImgIndex, false );
			
			// estimate the max warp 
			WarpMagnitudeSource<?> wmSrc = ((WarpMagnitudeSource<?>) sources.get( warpMagSourceIndex ).getSpimSource());
			double maxval = wmSrc.getMax( landmarkModel );
			
			// set the slider
			((RealARGBColorConverter< FloatType >)(sources.get( warpMagSourceIndex ).getConverter())).setMax(  maxval );
			
			vg.setFusedEnabled( true );
			viewerFrame.getViewerPanel().showMessage( "Displaying Warp Magnitude" );
			break;
		}
		case GRID: 
		{
			// turn grid vis on
			vg.setSourceActive( warpMagSourceIndex, false );
			vg.setSourceActive( gridSourceIndex, true );
			vg.setSourceActive( offImgIndex, false );
						
			vg.setFusedEnabled( true );
			viewerFrame.getViewerPanel().showMessage( "Displaying Warp Grid" );
			break;
		}
		default: {
			vg.setSourceActive( warpMagSourceIndex, false );
			vg.setSourceActive( gridSourceIndex, false );
			vg.setSourceActive( offImgIndex, true );
						
			vg.setFusedEnabled( false );
			viewerFrame.getViewerPanel().showMessage( "Turning off warp vis" );
			break;
		}
		}
	}
	
	public void toggleWarpVisMode( BigWarpViewerFrame viewerFrame )
	{
		int offImgIndex = 0;
		int onImgIndex  = 1;
		if( viewerFrame == null )
		{
			if( viewerFrameP.isActive() )
			{
				viewerFrame = viewerFrameP;
			}
			else if( viewerFrameQ.isActive() )
			{
				viewerFrame = viewerFrameQ;
			}else
				return;
		}
		
		if( viewerFrame == viewerFrameP )
		{
			offImgIndex = 1;
			onImgIndex  = 0;
		}
		
		if( landmarkModel.getTransform() == null )
		{
			viewerFrame.getViewerPanel().showMessage( "No warp - estimate warp first." );
			return;
		}
		
		VisibilityAndGrouping vg = viewerFrame.getViewerPanel().getVisibilityAndGrouping();
		
		// TODO consider remembering whether fused was on before displaying warpmag
		// so that its still on or off after we turn it off
		if( vg.isSourceActive( warpMagSourceIndex )) // warp mag is visible, turn it off
		{
			vg.setSourceActive( warpMagSourceIndex, false );
			
			vg.setSourceActive( offImgIndex, true );
			
			vg.setFusedEnabled( false );
			viewerFrame.getViewerPanel().showMessage( "Removing Warp Magnitude" );
		}
		else // warp mag is invisible, turn it on
		{
			vg.setSourceActive( warpMagSourceIndex, true );
			vg.setSourceActive( offImgIndex, false );
			
			// estimate the max warp 
			WarpMagnitudeSource<?> wmSrc = ((WarpMagnitudeSource<?>) sources.get( warpMagSourceIndex ).getSpimSource());
			double maxval = wmSrc.getMax( landmarkModel );
			
			// set the slider
			((RealARGBColorConverter< FloatType >)(sources.get( warpMagSourceIndex ).getConverter())).setMax(  maxval );
			
			vg.setFusedEnabled( true );
			viewerFrame.getViewerPanel().showMessage( "Displaying Warp Magnitude" );
		}
		
		viewerFrame.getViewerPanel().requestRepaint();
	}
	
	public void restimateTransformation()
	{
		
		if( landmarkModel.getRowCount() < 4 )
		{
			JOptionPane.showMessageDialog( landmarkFrame, "Require at least 4 points to estimate a transformation" );
			return;
		}
		
		getViewerFrameP().getViewerPanel().showMessage("Estimating transformation...");
		getViewerFrameQ().getViewerPanel().showMessage("Estimating transformation...");
		
		// TODO restimateTransformation
		boolean isFirst = false;
		if( landmarkModel.getTransform() == null )
		{
			landmarkModel.initTransformation();
			isFirst = true;
		}
		else
		{
			landmarkModel.transferUpdatesToModel();
		}
		
		// estimate the forward transformation
		landmarkModel.getTransform().solve();
		landmarkModel.resetWarpedPoints();
		landmarkModel.resetUpdated();
		
		( (WarpedSource<?>)(sources.get( 0 ).getSpimSource())).updateTransform( landmarkModel.getTransform().deepCopy() );
		
		if( sources.get(0).asVolatile() != null )
			( (WarpedSource<?>)(sources.get( 0 ).asVolatile().getSpimSource())).updateTransform( landmarkModel.getTransform().deepCopy() );
		
		// display the warped version automatically if this is the first
		// time the transform was computed
		if( isFirst )
			setIsMovingDisplayTransformed( true );
		
		WarpMagnitudeSource<?> wmSrc = ((WarpMagnitudeSource<?>) sources.get( warpMagSourceIndex ).getSpimSource());
		GridSource<?> gSrc = ((GridSource<?>) sources.get( gridSourceIndex ).getSpimSource());
		
		wmSrc.setWarp( landmarkModel.getTransform().deepCopy() );
		gSrc.setWarp( landmarkModel.getTransform().deepCopy() );
		fitBaselineWarpMagModel();
		
//		Interval warpedInterval = wmSrc.estimateBoundingInterval( 0, 0 );
//		System.out.println( "warped Interval:  [ " + warpedInterval.min( 0 ) + ", " + warpedInterval.max( 0 ) +
//				" ]  x  [ " + warpedInterval.min( 1 ) + ", " + warpedInterval.max( 1 ) +" ] ");
		
		viewerP.requestRepaint();
		viewerQ.requestRepaint();
	}
	
	public void setIsMovingDisplayTransformed( boolean isTransformed )
	{
		( (WarpedSource<?>)(sources.get( movingSourceIndex ).getSpimSource())).setIsTransformed( isTransformed );
		
		if( sources.get( movingSourceIndex ).asVolatile() != null )
			( (WarpedSource<?>)(sources.get( movingSourceIndex ).asVolatile().getSpimSource())).setIsTransformed( isTransformed );
		
		overlayP.setIsTransformed( isTransformed );
	}
	
	public boolean isMovingDisplayTransformed()
	{
		return ((WarpedSource<?>)(sources.get( movingSourceIndex ).getSpimSource())).isTransformed();
	}
	
	protected int detectNumDims()
	{
		// System.out.println( "ndim 0: " + sources.get( 0 ).getSpimSource().getSource( 0, 0 ).dimension( 2 ));
		
		boolean is1Src2d = sources.get( movingSourceIndex ).getSpimSource().getSource( 0, 0 ).dimension( 2 ) == 1;
		boolean is2Src2d = sources.get( fixedSourceIndex ).getSpimSource().getSource( 0, 0 ).dimension( 2 ) == 1;
		
		int ndims = 3;
		if( is1Src2d && is2Src2d )
			ndims = 2;
		
		return ndims;
	}
	
	
	public static void main( final String[] args )
	{
		//TODO main
		String fnP = "";
		String fnQ = "";
		String fnLandmarks = "";
		
		int i = 0;
		if( args.length >= 2 )
		{
			fnP = args[ i++ ];
			fnQ = args[ i++ ];
		}
		else
		{
			System.err.println("Must provide at least 2 inputs for moving and target image files");
			System.exit( 1 );
		}
		
		ArrayList< Double > resList = new ArrayList< Double >();
		
//		if( args.length >= 4 )
//		{
//			while( true )
//			{
//				
//				Double d;
//				try
//				{
//					d = Double.parseDouble( args[ i ]);
//					System.out.println("d: " + d );
//				}
//				catch(Exception e )
//				{
//					break;
//				}
//				resList.add( d );
//				i++;
//			}
//		}
		
		if( args.length > i )
			fnLandmarks = args[ i++ ];
		
		boolean doInverse = false;
		if( args.length > i )
		{
			try
			{
				doInverse = Boolean.parseBoolean( args[ i++ ] );
				System.out.println( "parsed inverse param: " + doInverse );
			}
			catch(Exception e)
			{
				// no inverse param
				System.err.println( "Warning: Failed to parse inverse parameter." );
			}
		}
		
		double[] resolutions = null;
		if( resList.size() > 2 )
		{
			resolutions = new double[ resList.size() ];
			for( int j = 0; j < resolutions.length; j++ )
				resolutions[ j ] = resList.get( j );
		}
		
		
		try
		{
			System.setProperty( "apple.laf.useScreenMenuBar", "false" );
			new RepeatingReleasedEventsFixer().install();
			
			BigWarp bw;
			if( fnP.endsWith("xml") && fnQ.endsWith("xml"))
				bw = new BigWarp( loadSourcesFromXmls( fnP, fnQ ), new File( fnP ).getName(), new ProgressWriterConsole() );
			else if( fnP.endsWith("png") && fnQ.endsWith("png") )
				bw = new BigWarp( loadSourcesFromImages( fnP, fnQ, resolutions ), new File( fnP ).getName(), new ProgressWriterConsole() );
			else{
				System.err.println("Error reading files - should both be xmls or both image files");
				return;
			}
			
			if( !fnLandmarks.isEmpty() )
				bw.landmarkModel.load( new File( fnLandmarks ));
			
			if( doInverse )
				bw.invertPointCorrespondences();
			
		}
		catch ( final Exception e )
		{
			
			e.printStackTrace();
		}
		
		System.out.println("done");
	}
	
	public void checkBoxInputMaps()
	{
		// Disable spacebar for toggling checkboxes
		// Make it enter instead
		// This is super ugly ... why does it have to be this way.
		
		TableCellEditor celled = landmarkTable.getCellEditor( 0, 1 );
		Component c = celled.getTableCellEditorComponent(landmarkTable, new Boolean(true), true, 0, 1 );
		
		InputMap parentInputMap = ((JCheckBox)c).getInputMap().getParent();
		parentInputMap.clear();
		KeyStroke enterDownKS = KeyStroke.getKeyStroke("pressed ENTER" );
		KeyStroke enterUpKS = KeyStroke.getKeyStroke("released ENTER" );
		
		parentInputMap.put( enterDownKS, "pressed" );
		parentInputMap.put(   enterUpKS, "released" );
		
		/* Consider with replacing with something like the below 
		 * Found in BigWarpViewerFrame
		 */
//		SwingUtilities.replaceUIActionMap( getRootPane(), keybindings.getConcatenatedActionMap() );
//		SwingUtilities.replaceUIInputMap( getRootPane(), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keybindings.getConcatenatedInputMap() );
	}
	
	public static BigWarpData loadSourcesFromImages( final String filenameP, final String filenameQ, double[] resolutions )
	{
		ImagePlus moving_imp = IJ.openImage( filenameP );
		ImagePlus target_imp = IJ.openImage( filenameQ );
		
		return BigWarpImagePlusPlugIn.buildData( moving_imp, target_imp, resolutions );
	}
	
	public static BigWarpData loadSourcesFromXmls( final String xmlFilenameP, final String xmlFilenameQ )
	{
		/* Load the first source */
		final SpimDataMinimal spimDataP = loadSpimData( xmlFilenameP );
		final SpimDataMinimal spimDataQ = loadSpimData( xmlFilenameQ );
		final AbstractSequenceDescription< ?, ?, ? > seqP = spimDataP.getSequenceDescription();
		final AbstractSequenceDescription< ?, ?, ? > seqQ = spimDataQ.getSequenceDescription();
		
		final ArrayList< ConverterSetup > converterSetups = new ArrayList< ConverterSetup >();
		
		ArrayList< SourceAndConverter< ? > > sources = new ArrayList< SourceAndConverter< ? > >();
		BigWarpInit.initSetups( spimDataP, converterSetups, sources );

		/* Load the second source */
		BigWarpInit.initSetups( spimDataQ, converterSetups, sources );
		
		return new BigWarpData( sources, seqP, seqQ, converterSetups );
	}

	public static class BigWarpData
	{
		public final ArrayList< SourceAndConverter< ? >> sources;
		public final AbstractSequenceDescription< ?, ?, ? > seqP;
		public final AbstractSequenceDescription< ?, ?, ? > seqQ;
		public final ArrayList< ConverterSetup > converterSetups;
		
		public BigWarpData( ArrayList< SourceAndConverter< ? >> sources,
				AbstractSequenceDescription< ?, ?, ? > seqP,
				AbstractSequenceDescription< ?, ?, ? > seqQ,
				ArrayList< ConverterSetup > converterSetups )
		{
			this.sources = sources;
			this.seqP = seqP;
			this.seqQ = seqQ;
			this.converterSetups = converterSetups;
		}
	}
	
	protected class MouseLandmarkListener implements MouseListener, MouseMotionListener
	{

		// -1 indicates that no point is selected
		int selectedPointIndex = -1;
		double[] ptarrayLoc = new double[ 3 ];
		double[] ptBackLoc = new double[ 3 ];
		
		private BigWarpViewerPanel thisViewer;
		private boolean isMoving;
		
		protected MouseLandmarkListener( BigWarpViewerPanel thisViewer )
		{
			setViewer( thisViewer );
			thisViewer.getDisplay().addHandler( this );
			isMoving = ( thisViewer == BigWarp.this.viewerP );
		}
		
		protected void setViewer( BigWarpViewerPanel thisViewer )
		{
			this.thisViewer = thisViewer;
		}
		
		@Override
		public void mouseClicked(MouseEvent arg0) {}
		
//		/**
//		 * Returns the index of the landmark under the mouse position,
//		 * or -1 if no landmark is at the current position
//		 */
		protected int selectedLandmarkLocal( double[] pt, boolean isMoving )
		{
			int N = BigWarp.this.landmarkModel.getRowCount();
			
			double dist = 0;
			double radsq = 100;
			landmarkLoop:
			for( int n = 0; n < N; n++ )
			{
			
				dist = 0;
				Double[] lmpt = BigWarp.this.landmarkModel.getPoints( isMoving ).get(n);
				
				for( int i = 0; i < ndims; i++ )
				{
					dist += (pt[i] - lmpt[i]) * (pt[i] - lmpt[i]);

					if( dist > radsq )
						continue landmarkLoop;
				}
				
				if( BigWarp.this.landmarkFrame.isVisible() ){
					BigWarp.this.landmarkTable.setEditingRow( n );
					BigWarp.this.landmarkFrame.repaint();
				}
				return n;
			}
			return -1;
		}
		
		@Override
		public void mouseEntered( MouseEvent arg0 ) {}

		@Override
		public void mouseExited( MouseEvent arg0 ) {}

		@Override
		public void mousePressed( MouseEvent e ) 
		{
			// shift down is reserved for drag overlay
	    	if( e.isShiftDown() )
	    	{
	    		return;
	    	}
	    	
			if( BigWarp.this.inLandmarkMode )
			{
				// TODO landmark pressed
				
				thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
				BigWarp.this.currentLandmark.localize( ptarrayLoc );	
				
				selectedPointIndex = BigWarp.this.updateWarpedPoint( ptarrayLoc, isMoving );
				
				//System.out.println( "clicked: " + XfmUtils.printArray(ptarrayLoc) );
				//System.out.println( "selected Point: " + selectedPointIndex );
			}
		}

		@Override
		public void mouseReleased( MouseEvent e ) 
		{
			// shift down is reserved for drag overlay
	    	if( e.isShiftDown() )
	    	{
	    		return;
	    	}
	    	
	    	if( e.isControlDown() )
	    	{
	    		if( BigWarp.this.inLandmarkMode && selectedPointIndex < 0 )
	    		{
	    			thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
	    			addFixedPoint( BigWarp.this.currentLandmark, isMoving );
	    		}
	    		return;
	    	}
	    	
			// deselect any point that may be selected
			if ( BigWarp.this.inLandmarkMode )
			{
				thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
				currentLandmark.localize( ptarrayLoc );

				if ( selectedPointIndex == -1 )
				{
					String message = addPoint( ptarrayLoc, isMoving );
					
//					if( isMoving && landmarkModel.getTransform() != null && viewerP.getTransformEnabled())
//					{
//						System.out.println( "updateWarpedPoint" );
//						landmarkModel.updateWarpedPoint( selectedPointIndex, ptarrayLoc );
//					}

					if ( !message.isEmpty() )
						thisViewer.showMessage( message );
				} 
				else
				{
					
					if( isMoving && landmarkModel.getTransform() != null && BigWarp.this.isMovingDisplayTransformed() )
					{
						//System.out.println("Add point moving, warped space");
						landmarkModel.getTransform().apply( ptarrayLoc, ptBack );
						BigWarp.this.landmarkModel.setPoint( selectedPointIndex, isMoving, ptBack );
					}
					else
					{
						BigWarp.this.landmarkModel.setPoint( selectedPointIndex, isMoving, ptarrayLoc );
					}
//					BigWarp.this.landmarkModel.setPoint( selectedPointIndex, isMoving, ptarrayLoc );
				}
			}
			selectedPointIndex = -1;
			//System.out.println( "\n\n" ); 
		}

		@Override
		public void mouseDragged(MouseEvent e) 
		{
			// shift down is reserved for drag overlay
	    	if( e.isShiftDown() )
	    	{
	    		return;
	    	}
	    	
			if( BigWarp.this.inLandmarkMode && selectedPointIndex >= 0 )
			{
				BigWarp.this.landmarkModel.setPreDraggedPoint( selectedPointIndex, isMoving );
				
				thisViewer.getGlobalMouseCoordinates( BigWarp.this.currentLandmark );
				currentLandmark.localize( ptarrayLoc );
				
				updatePointLocation( ptarrayLoc, isMoving, selectedPointIndex );
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {}
		
		/**
		 * Adds a point in the moving and fixed images at the same point.  
		 */
		public void addFixedPoint( RealPoint pt, boolean isMovingImage )
		{
			if( isMovingImage && viewerP.getTransformEnabled() )
			{
				// Here we clicked in the space of the moving image
				currentLandmark.localize( ptarrayLoc );
				addPoint( ptarrayLoc,  true, viewerP );
				addPoint( ptarrayLoc, false, viewerQ );
			}
			else
			{
				currentLandmark.localize( ptarrayLoc );
				addPoint( ptarrayLoc,  true, viewerP );
				addPoint( ptarrayLoc, false, viewerQ );
			}
		}
	}
	
	public class LandmarkTableListener implements TableModelListener 
	{
		@Override
		public void tableChanged(TableModelEvent arg0) 
		{
			BigWarp.this.viewerP.requestRepaint();
			BigWarp.this.viewerQ.requestRepaint();
		}
	}
	
	public class MouseLandmarkTableListener implements MouseListener
	{
		
		boolean wasDoubleClick = false;
	    Timer timer;
	    
	    public MouseLandmarkTableListener() {}
	 
	    public void mouseClicked( MouseEvent e)
	    {
	    	
	    	if( BigWarp.this.inLandmarkMode )
	    	{
	    		JTable target = (JTable)e.getSource();
	    		
				int row = target.getSelectedRow();
				int column = target.getSelectedColumn();
	    		
	    		boolean isMoving = ( column > 1 && column < ( 2 + ndims ) );
	    		
	    		BigWarp.this.landmarkModel.setPointToUpdate( row, isMoving );
	    		
	    	}
	    	else if( e.getClickCount() == 2 )
	    	{
				JTable target = (JTable)e.getSource();
				int row = target.getSelectedRow();
				int column = target.getSelectedColumn();

				double[] pt = null;
				int offset = 0;
				
				
				final BigWarpViewerPanel viewer;
				if( column >= ( 2 + ndims ) )
				{
					viewer = BigWarp.this.viewerQ;
					offset = ndims;
				}
				else if( column >= 2 && column < ( 2 + ndims ))
				{
					viewer = BigWarp.this.viewerP;
					
					if( BigWarp.this.viewerP.getOverlay().getIsTransformed() )
						offset = ndims; 
				}
				else
				{
					// we're in a column that doesnt correspond to a point and
					// should do nothing
					return;
				}
				
				if( ndims == 3 )
				{
					pt = new double[]{
						(Double)target.getValueAt( row, offset + 2 ),
						(Double)target.getValueAt( row, offset + 3 ),
						(Double)target.getValueAt( row, offset + 4 )};
				}
				else
				{
					pt = new double[]{
							(Double)target.getValueAt( row, offset + 2 ),
							(Double)target.getValueAt( row, offset + 3 ), 0.0};
				}
				
				// we have an unmatched point
				if( Double.isInfinite( pt[ 0 ]))
					return;
				
				
				final AffineTransform3D transform = viewer.getDisplay().getTransformEventHandler().getTransform();
				AffineTransform3D xfmCopy = transform.copy();
				xfmCopy.set( 0.0, 0, 3 );
				xfmCopy.set( 0.0, 1, 3 );
				xfmCopy.set( 0.0, 2, 3 );
				
				double[] center = new double[]{
						viewer.getWidth() / 2,
						viewer.getHeight() / 2,
						0
				};
				double[] ptxfm = new double[3];
				xfmCopy.apply( pt, ptxfm );
				
				// this should work fine in the 2d case
				TranslationAnimator animator = new TranslationAnimator( 
						transform, 
						new double[]{center[0]-ptxfm[0], center[1]-ptxfm[1], -ptxfm[2]}, 
						300 );
				viewer.setTransformAnimator( animator );
				viewer.transformChanged( transform );
				
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {}

	}
	
	protected static class DummyTransformEventHandler implements TransformEventHandler<AffineTransform3D>
	{

		@Override
		public AffineTransform3D getTransform() {
			return null;
		}

		@Override
		public void setTransform(AffineTransform3D transform) {}

		@Override
		public void setCanvasSize(int width, int height, boolean updateTransform) {}

		@Override
		public void setTransformListener(
				TransformListener<AffineTransform3D> transformListener) {	
		}

		@Override
		public String getHelpString() {
			return null;
		}
		
		public String toString(){
			return "Dummy Transform Handler";
		}
	}
}
