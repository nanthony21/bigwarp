/*-
 * #%L
 * BigWarp plugin for Fiji.
 * %%
 * Copyright (C) 2015 - 2021 Howard Hughes Medical Institute.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package bigwarp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Enumeration;

import javax.swing.AbstractButton;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import bdv.tools.brightness.SliderPanelDouble;
import bdv.util.BoundedValueDouble;
import bdv.viewer.BigWarpViewerSettings;
import bigwarp.source.GridSource;
import ij.IJ;

public class WarpVisFrame extends JDialog 
{
	private static final long serialVersionUID = 7561228647761694686L;

	private final BigWarp bw;
	private final BigWarpViewerSettings settings;
	
	protected ButtonGroup visTypeGroup;
	protected JRadioButton setWarpVisOffButton;
	protected JRadioButton setWarpGridButton;
	protected JRadioButton setWarpMagButton;
	
	protected JLabel noOptionsLabel;
	
	// landmark point options
	protected final JButton landmarkColorButton;
	private final JColorChooser colorChooser;
	protected final JSlider landmarkSizeSlider;
	
	// warp magnitude
	protected ButtonGroup warpMagButtons;
	protected JRadioButton warpMagAffineButton;
	protected JRadioButton warpMagSimilarityButton;
	protected JRadioButton warpMagRigidButton;
	
	// grid spacing
	protected ButtonGroup warpGridButtons;
	protected JRadioButton warpGridLineButton;
	protected JRadioButton warpGridModButton;
	protected JSlider gridSpacingSlider;
	protected JSlider gridWidthSlider;
	protected Component	 bigSpace;
	protected Component	 smallSpace;
	protected JLabel gridSpacingLabel;
	protected JLabel gridWidthLabel;


	// autosave
	final SpinnerNumberModel savePeriodModel;
	final JSpinner autoSavePeriodSpinner;
	final JCheckBox doAutoSaveBox;
	final JTextField autoSaveFolderText;

	public static final int minGridSpacing = 5;
	public static final int maxGridSpacing = 400;
	public static final int defaultGridSpacing = 100;
	
	public static final int minGridWidth = 1;
	public static final int maxGridWidth = 50;
	public static final int defaultGridWidth = 5;
	
	public WarpVisFrame( final Frame owner, final BigWarp<?> bw )
	{
		super( owner, "big warp options", false );
		this.bw = bw;
		this.settings = bw.viewerSettings;
		
		final Container content = getContentPane();
		
		setSize( 500, 400 );
		
		JPanel landmarkPointOptionsPanel = new JPanel();
		landmarkPointOptionsPanel.setLayout( new BoxLayout( landmarkPointOptionsPanel, BoxLayout.X_AXIS ));

		landmarkColorButton = new JButton( new ColorIcon( settings.getSpotColor() ) );
		colorChooser = new JColorChooser();
		
		landmarkSizeSlider = new JSlider();
		landmarkSizeSlider.setValue( (int)settings.getSpotSize() );
		landmarkSizeSlider.setMinimum( 1 );
		landmarkSizeSlider.setMaximum( 20 );
		
		landmarkPointOptionsPanel.add( landmarkColorButton );
		landmarkPointOptionsPanel.add( landmarkSizeSlider );
		landmarkPointOptionsPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"landmark size & color" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		
		// 
		JPanel visTypePanel = new JPanel();
		visTypePanel.setLayout(  new BoxLayout( visTypePanel, BoxLayout.Y_AXIS) );
		visTypePanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"warp display" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		
		JPanel typeOptionPanel = new JPanel();
		typeOptionPanel.setLayout(  new BoxLayout( typeOptionPanel, BoxLayout.Y_AXIS) );
		typeOptionPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"options" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		
		// label indicating that there are no options to be had
		noOptionsLabel = new JLabel( "None" );
		
		// buttons choosing if and how the warp should be visualized
		visTypeGroup = new ButtonGroup();
		setWarpVisOffButton = new JRadioButton( "Off" );
		setWarpGridButton = new JRadioButton( "Grid" );
		setWarpMagButton = new JRadioButton( "Magnitude" );
		
		visTypeGroup.add( setWarpVisOffButton );
		visTypeGroup.add( setWarpGridButton );
		visTypeGroup.add( setWarpMagButton );
		
		visTypePanel.add( setWarpVisOffButton );
		visTypePanel.add( setWarpGridButton );
		visTypePanel.add( setWarpMagButton );
		
		
		// buttons for warp magnitude options
		warpMagAffineButton = new JRadioButton( "Affine baseline" );
		warpMagSimilarityButton = new JRadioButton("Similarity baseline");
		warpMagRigidButton = new JRadioButton("Rigid baseline");

		warpMagButtons = new ButtonGroup();
		warpMagButtons.add( warpMagAffineButton );
		warpMagButtons.add( warpMagSimilarityButton );
		warpMagButtons.add( warpMagRigidButton );
		
		// buttons for warp grid options 
		warpGridLineButton = new JRadioButton( "Line grid " );
		warpGridModButton  = new JRadioButton( "Modulo grid" );
		
		warpGridButtons = new ButtonGroup();
		warpGridButtons.add( warpGridLineButton );
		warpGridButtons.add( warpGridModButton );
		
		gridSpacingSlider = new JSlider( JSlider.HORIZONTAL, minGridSpacing, maxGridSpacing, defaultGridSpacing );
		gridWidthSlider = new JSlider( JSlider.HORIZONTAL, minGridWidth, maxGridWidth, defaultGridWidth );
		// label the sliders
		gridSpacingLabel = new JLabel("Grid Spacing", JLabel.CENTER);
		gridWidthLabel = new JLabel("Grid Width", JLabel.CENTER);
		gridSpacingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		gridWidthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		bigSpace = Box.createVerticalStrut( 20 );
		smallSpace = Box.createVerticalStrut( 10 );

		typeOptionPanel.add( warpMagAffineButton );
		typeOptionPanel.add( warpMagSimilarityButton );
		typeOptionPanel.add( warpMagRigidButton );
		
		typeOptionPanel.add( warpGridLineButton );
		typeOptionPanel.add( warpGridModButton );
		typeOptionPanel.add( bigSpace );
		typeOptionPanel.add( gridSpacingLabel );
		typeOptionPanel.add( gridSpacingSlider );
		typeOptionPanel.add( smallSpace );
		typeOptionPanel.add( gridWidthLabel );
		typeOptionPanel.add( gridWidthSlider );
		typeOptionPanel.add( noOptionsLabel );

		// autoSave Options panel
		final JPanel autoSaveOptionsPanel = new JPanel();
		autoSaveOptionsPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"Auto-save options" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );
		autoSaveOptionsPanel.setLayout( new GridBagLayout() );

		final JLabel autosaveLabel = new JLabel("Auto-save landmarks");
		doAutoSaveBox = new JCheckBox();

		final JLabel autoSavePeriodLabel = new JLabel("save frequency (minutes)");
		autoSavePeriodSpinner = new JSpinner();
		savePeriodModel = new SpinnerNumberModel( 5, 1, 5000, 1 );
		autoSavePeriodSpinner.setModel( savePeriodModel );
		autoSavePeriodSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				if( doAutoSaveBox.isSelected() )
				{
					long periodMillis = ( ( Integer ) savePeriodModel.getValue() ).longValue() * 60000;
					BigWarpAutoSaver autoSaver = bw.getAutoSaver();
					if ( autoSaver != null )
						autoSaver.stop();

					new BigWarpAutoSaver( bw, periodMillis );
				}
			}
		} );

		doAutoSaveBox.addItemListener( new ItemListener()
		{
			@Override
			public void itemStateChanged( ItemEvent e )
			{
				bw.stopAutosave();

				if ( doAutoSaveBox.isSelected() )
				{
					long periodMillis = ( ( Integer ) savePeriodModel.getValue() ).longValue() * 60000;
					new BigWarpAutoSaver( bw, periodMillis );
				}
			}
		});

		final JLabel destDirLabel = new JLabel("Directory");
		final File startingFolder = bw.getBigwarpSettingsFolder();
		autoSaveFolderText = new JTextField();
		autoSaveFolderText.setText( startingFolder.getAbsolutePath() );

		final JButton browseBtn = new JButton( "Browse" );
		browseBtn.addActionListener( e -> {

			final JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
			fileChooser.setCurrentDirectory( startingFolder );

			final int ret = fileChooser.showOpenDialog(content); 
			if (ret == JFileChooser.APPROVE_OPTION)
			{
				final File folder = fileChooser.getSelectedFile();
				autoSaveFolderText.setText( folder.getAbsolutePath() );
				bw.setAutosaveFolder( folder );
			}
		});

		final GridBagConstraints gbcAutoSave = new GridBagConstraints();
		gbcAutoSave.gridx = 0;
		gbcAutoSave.gridy = 0;
		gbcAutoSave.gridwidth = 2;
		gbcAutoSave.gridheight = 1;
		gbcAutoSave.weightx = 1.0;
		gbcAutoSave.weighty = 0.0;
		gbcAutoSave.anchor = GridBagConstraints.EAST;
		gbcAutoSave.fill = GridBagConstraints.HORIZONTAL;
		gbcAutoSave.insets = new Insets( 5, 5, 5, 5 );

		autoSaveOptionsPanel.add( autosaveLabel, gbcAutoSave ); 

		gbcAutoSave.gridx = 2;
		gbcAutoSave.gridwidth = 1;
		gbcAutoSave.weightx = 0.0;
		gbcAutoSave.anchor = GridBagConstraints.WEST;
		autoSaveOptionsPanel.add( doAutoSaveBox, gbcAutoSave );

		gbcAutoSave.weightx = 1.0;
		gbcAutoSave.gridx = 0;
		gbcAutoSave.gridwidth = 2;
		gbcAutoSave.gridy = 1;
		gbcAutoSave.anchor = GridBagConstraints.EAST;
		autoSaveOptionsPanel.add( autoSavePeriodLabel, gbcAutoSave );

		gbcAutoSave.weightx = 0.0;
		gbcAutoSave.gridx = 2;
		gbcAutoSave.anchor = GridBagConstraints.WEST;
		autoSaveOptionsPanel.add( autoSavePeriodSpinner, gbcAutoSave );

		gbcAutoSave.gridy = 2;
		gbcAutoSave.gridx = 0;
		gbcAutoSave.gridwidth = 1;
		gbcAutoSave.weightx = 0.0;
		gbcAutoSave.anchor = GridBagConstraints.EAST;
		autoSaveOptionsPanel.add( destDirLabel, gbcAutoSave );

		gbcAutoSave.gridy = 2;
		gbcAutoSave.gridx = 1;
		gbcAutoSave.weightx = 1.0;
		gbcAutoSave.anchor = GridBagConstraints.EAST;
		autoSaveOptionsPanel.add( autoSaveFolderText, gbcAutoSave );

		gbcAutoSave.gridx = 2;
		gbcAutoSave.weightx = 0.0;
		gbcAutoSave.anchor = GridBagConstraints.WEST;
		autoSaveOptionsPanel.add( browseBtn, gbcAutoSave );



		final JPanel inverseOptionsPanel = new JPanel();
		inverseOptionsPanel.setLayout( new BorderLayout( 10, 10 ));

		inverseOptionsPanel.setBorder( BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder( 4, 2, 4, 2 ),
				BorderFactory.createCompoundBorder(
						BorderFactory.createTitledBorder(
								BorderFactory.createEtchedBorder(),
								"Inverse options" ),
						BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) ) );

		final JPanel tolerancePanel = new JPanel();
		final JSpinner toleranceSpinner = new JSpinner();
		final SpinnerNumberModel tolmodel = new SpinnerNumberModel( 0.5, 0.001, 200.0, 0.1 );
		toleranceSpinner.setModel( tolmodel );
		toleranceSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				bw.getLandmarkPanel().getTableModel().setInverseThreshold( (Double)toleranceSpinner.getValue() );
			}
		} );
		tolerancePanel.add( new JLabel( "Inverse error", SwingConstants.CENTER ), BorderLayout.WEST );
		tolerancePanel.add( toleranceSpinner, BorderLayout.EAST );

		final JPanel maxIterPanel = new JPanel();
		final JSpinner maxIterSpinner = new JSpinner();
		final SpinnerNumberModel itermodel = new SpinnerNumberModel( 200, 1, 5000, 1 );
		maxIterSpinner.setModel( itermodel );
		maxIterSpinner.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				bw.getLandmarkPanel().getTableModel().setMaxInverseIterations( (Integer)maxIterSpinner.getValue() );
			}
		} );
		maxIterPanel.add( new JLabel( "Max iterations", SwingConstants.CENTER ), BorderLayout.WEST );
		maxIterPanel.add( maxIterSpinner, BorderLayout.EAST );
		
		inverseOptionsPanel.add( tolerancePanel, BorderLayout.NORTH );
		inverseOptionsPanel.add( maxIterPanel, BorderLayout.SOUTH );

		content.setLayout( new GridBagLayout() );

		final GridBagConstraints gbcContent = new GridBagConstraints();
		gbcContent.gridx = 0;
		gbcContent.gridy = 0;
		gbcContent.gridwidth = 3;
		gbcContent.fill = GridBagConstraints.HORIZONTAL;
		gbcContent.anchor = GridBagConstraints.CENTER;
		gbcContent.weightx = 1.0;
		gbcContent.weighty = 1.0;
		gbcContent.insets = new Insets( 1, 1, 1, 1 );
		content.add( landmarkPointOptionsPanel, gbcContent );

		gbcContent.gridx = 0;
		gbcContent.gridy = 1;
		gbcContent.gridwidth = 1;
		gbcContent.anchor = GridBagConstraints.WEST;
		content.add( visTypePanel, gbcContent );

		gbcContent.gridy = 1;
		gbcContent.gridx = 1;
		gbcContent.gridwidth = 2;
		gbcContent.anchor = GridBagConstraints.EAST;
		content.add( typeOptionPanel, gbcContent );

		gbcContent.gridx = 0;
		gbcContent.gridy = 2;
		gbcContent.gridwidth = 3;
		content.add( inverseOptionsPanel, gbcContent );

		gbcContent.gridy = 3;
		content.add( autoSaveOptionsPanel , gbcContent );
		
		setDefaultCloseOperation( WindowConstants.HIDE_ON_CLOSE );
		
		addListeners();
		updateOptions();
	}

	public void setActions()
	{
		final ActionMap actionMap = bw.getViewerFrameP().getKeybindings().getConcatenatedActionMap();

		landmarkColorButton.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				colorChooser.setColor( settings.getSpotColor());
				final JDialog d = JColorChooser.createDialog( landmarkColorButton, "Choose a color", true, colorChooser, new ActionListener()
				{
					@Override
					public void actionPerformed( final ActionEvent arg0 )
					{
						final Color c = colorChooser.getColor();
						if (c != null)
						{
							landmarkColorButton.setIcon( new ColorIcon( c ) );
							bw.setSpotColor( c );
						}
					}
				}, null );
				d.setVisible( true );
			}
		} );
		
		landmarkSizeSlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				if( e.getSource() != landmarkSizeSlider ) return;
				
				settings.setSpotSize( landmarkSizeSlider.getValue() );
				bw.viewerP.requestRepaint();
				bw.viewerQ.requestRepaint();
			}
		});
		
		setWarpVisOffButton.setAction( 
				actionMap.get( String.format( BigWarpActions.SET_WARPTYPE_VIS, BigWarp.WarpVisType.NONE )));
		setWarpGridButton.setAction( 
				actionMap.get( String.format( BigWarpActions.SET_WARPTYPE_VIS, BigWarp.WarpVisType.GRID )));
		setWarpMagButton.setAction( 
				actionMap.get( String.format( BigWarpActions.SET_WARPTYPE_VIS, BigWarp.WarpVisType.WARPMAG )));
		
		setWarpVisOffButton.setText("Off");
		setWarpGridButton.setText("Grid");
		setWarpMagButton.setText("Magnitude");
		
		warpMagAffineButton.setAction( 
				actionMap.get( String.format( BigWarpActions.WARPMAG_BASE, bw.baseXfmList[ 0 ].getClass().getName() )));
		warpMagSimilarityButton .setAction( 
				actionMap.get( String.format( BigWarpActions.WARPMAG_BASE, bw.baseXfmList[ 1 ].getClass().getName() ) ));
		warpMagRigidButton.setAction( 
				actionMap.get( String.format( BigWarpActions.WARPMAG_BASE, bw.baseXfmList[ 2 ].getClass().getName() ) ));
		
		warpMagAffineButton.setText("Affine");
		warpMagSimilarityButton.setText("Similarity");
		warpMagRigidButton.setText("Rigid");
		
		warpGridLineButton.setAction( 
				actionMap.get( String.format( BigWarpActions.WARPVISGRID, GridSource.GRID_TYPE.LINE )));
		warpGridModButton.setAction( 
				actionMap.get( String.format( BigWarpActions.WARPVISGRID, GridSource.GRID_TYPE.MOD )));
		
		warpGridLineButton.setText( "Line" );
		warpGridModButton.setText( "Modulo" );
		
		// turn on the default values
		setWarpVisOffButton.doClick();
		warpMagAffineButton.doClick();
		warpGridLineButton.doClick();
	}
	
	public void addListeners()
	{
		MyChangeListener mylistener = new MyChangeListener();
		setWarpVisOffButton.addChangeListener( mylistener );
		setWarpGridButton.addChangeListener( mylistener );
		setWarpMagButton.addChangeListener( mylistener );
		
		gridSpacingSlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				if( e.getSource() != gridSpacingSlider ) return;
				
				WarpVisFrame.this.bw.setWarpGridSpacing( gridSpacingSlider.getValue() );
			}
		});
		
		gridWidthSlider.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( ChangeEvent e )
			{
				if( e.getSource() != gridWidthSlider ) return;
				
				WarpVisFrame.this.bw.setWarpGridWidth( gridWidthSlider.getValue() );
			}
		});
	}
	
	public class MyChangeListener implements ChangeListener
	{
		@Override
		public void stateChanged( ChangeEvent e )
		{
			WarpVisFrame.this.updateOptions();
		}
	}
	
	private void setGridOptionsVisibility( boolean isVisible )
	{
		// disable all options
		Enumeration< AbstractButton > elems = warpGridButtons.getElements();
		while( elems.hasMoreElements())
			elems.nextElement().setVisible( isVisible ); 

		gridSpacingSlider.setVisible( isVisible );
		gridWidthSlider.setVisible( isVisible );
		bigSpace.setVisible( isVisible );
		gridSpacingLabel.setVisible( isVisible );
		gridWidthLabel.setVisible( isVisible );
	}
	
	private void setMagOptionsVisibility( boolean isVisible )
	{
		// disable all options
		Enumeration< AbstractButton > elems = warpMagButtons.getElements();
		while( elems.hasMoreElements())
			elems.nextElement().setVisible( isVisible ); 
	}
	
	public synchronized void updateOptions()
	{
		if( setWarpVisOffButton.isSelected() )
		{
			noOptionsLabel.setVisible( true );
			setGridOptionsVisibility( false );
			setMagOptionsVisibility( false );
		}
		else if( setWarpGridButton.isSelected() )
		{
			noOptionsLabel.setVisible( false );
			setGridOptionsVisibility( true );
			setMagOptionsVisibility( false );
		}
		else if( setWarpMagButton.isSelected() )
		{
			noOptionsLabel.setVisible( false );
			setGridOptionsVisibility( false );
			setMagOptionsVisibility( true );
		}
		else
		{
//			System.out.println(" UHOH ");
		}
		pack();
	}
	
	private static class ColorIcon implements Icon
	{
		private final int size = 16;

		private final Color color;

		public ColorIcon( final Color color )
		{
			this.color = color;
		}

		@Override
		public void paintIcon( final Component c, final Graphics g, final int x, final int y )
		{
			final Graphics2D g2d = ( Graphics2D ) g;
			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
			g2d.setColor( color );
			g2d.fillOval( x, y, size, size );
		}

		@Override
		public int getIconWidth()
		{
			return size;
		}

		@Override
		public int getIconHeight()
		{
			return size;
		}
	}
}
