/*
 * Copyright (C) 2011 Thomas Barrasso
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This code extends the Android RelativeLayout widget. This layout was created
 * by Thomas Barrasso, contact 'at' tombarrasso dot com.
 *
 * @author Thomas Barrasso
 */
package com.sunshowerstudios.android.npuzzlesimple;

// Java packages
import java.io.FileNotFoundException;
import java.util.Random;

// Android packages
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/*
 * A custom Android view that implements the nPuzzle
 * game. Supply an image source to play the game, and
 * an onSolveListener. This view creates a RelativeLayout
 * which inside contains a TextView for the count down and
 * and TableLayout for the puzzle pieces. OnClick events
 * are handled against current game state for each move,
 * and when the state matches the unshuffled state the
 * user has solved the puzzle and onSolve is triggered.
 */

public class NPuzzleView extends RelativeLayout
{
	// @Private
	private static final boolean ZERO_IN_CORNER = false;	// Whether or not the blank piece NEEDS to be in the corner.
	private static int COUNTDOWN_SECONDS = 3,				// The number of seconds that the initial count down lasts.
					   BORDER_WIDTH = 6;					// The width in pixels of the border bitmap.
	private Context mContext; 								// Reference to the current context of this Activity.
	private TableLayout mTableLayout; 						// Reference to containing TableLayout of pieces.
	private TextView mCountdown; 							// Reference to TextView used in counting down.
	private static Drawable mBorder; 						// Drawable resource of the border.
	private static Bitmap mBitmap, mBlankBitmap; 			// Original bitmap of image & scaled version.
	private static int[][] gameState; 						// Multidimension array of board.
	private boolean isSolved, 								// Used to prevent clicking after the game has been solved,
					isCounting,								// or during the initial count down.
					firstSizeChange = true,					// Used to prevent initial size change handling.
					initialized = false;					// Whether or not init() has been called.
	private DisplayMetrics metrics = new DisplayMetrics(); 	// Window dimensions.
	private int[] dims, 									// Image dimensions based on those of the window.
				  originalDims = new int[2], 				// Dimensions of the original image.
				  pieceDims = new int[2]; 					// Dimensions of each game piece.
	private static int difficulty = 3, 						// Difficulty level, ie. 3 -> 3 x 3 board.
			   moves = 0;									// Number of moves thus far.
	private OnSolveListener onSolveListener 				// Handles when the puzzle is solved.
					= new OnSolveListener() 				// the onSolve method is called.
	{
		public void onSolve() {}
	};
	
	/* ====================
	 *    Public Methods
	 * ====================
	 * 
	 * return type	method signature
	 * 
	 * NPuzzleView 	NPuzzleView(Context context);
	 * NPuzzleView 	NPuzzleView(Context context, AttributeSet attrs);
	 * void 		setDifficulty(int N);
	 * void 		setImage(Bitmap image);
	 * void			setImageUri(Uri uri);
	 * void			setCountDownSeconds(int seconds);
	 * int			getCountDownSeconds();
	 * void 		reload();
	 * void			mix(boolean mix);
	 * int			getDifficulty();
	 * int			getMoves();
	 * TextView		getCountDownTextView();
	 * void			setBorder(Drawable border);
	 */
	
	// ====================
	//    Public Methods
	// ====================
	
	// Setter for difficulty.
	public void setDifficulty(int N)
	{
		difficulty = N;
	}
	
	// Setter of mBitmap from bitmap.
	public void setImage(Bitmap image)
	{
		if (image == null) return;
		
		mBitmap = image;
		
    	// Save its original dimensions.
    	originalDims[0] = image.getWidth();
    	originalDims[1] = image.getHeight();
	}
	
	// Setter of mBitmap using a URI.
	public void setImageUri(Uri uri) throws FileNotFoundException
	{
		setImage(BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(uri)));
	}
	
	// Setter of count down seconds.
	public void setCountDownSeconds(int seconds)
	{
		COUNTDOWN_SECONDS = seconds;
	}
	
	// Getter of count down seconds.
	public int getCountDownSeconds()
	{
		return COUNTDOWN_SECONDS;
	}
	
	// Reloads current game, starts all over.
	public void reload()
	{
		moves = 0;
		isSolved = false;
		countDown();
	}
	
	// Mixes the game.
	public void mix(boolean mix)
	{
		if (mix)
		{
			isCounting = true;
			isSolved = false;
	        shuffle(false);
	        drawTable();
			countDown();
		}
		else
		{
			shuffle(false);
			drawTable();
		}
	}
	
	// Returns moves.
	public int getMoves()
	{
		return moves;
	}
	
	// Returns difficulty.
	public int getDifficulty()
	{
		return difficulty;
	}
	
	// Setter for OnSolverListener.
	public void setOnSolveListener(OnSolveListener osl)
	{
		onSolveListener = osl;
	}
	
	// Setter for mBorder.
	public void setBorder(Drawable border)
	{
		mBorder = border;
	}
	
	// Getter for OnSolveListener.
	public OnSolveListener getOnSolveListener()
	{
		return onSolveListener;
	}

	// Returns count down TextView.
	public TextView getCountDownTextView()
	{
		return mCountdown;
	}
	
	// ====================
	//     Constructors
	// ====================
	
	// Constructed without attributes, ie. programmatically.
	public NPuzzleView(Context context)
	{
		super(context);
		mContext = context;
		init();
	}
	
	// Constructed with attributes.
	public NPuzzleView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
				
		// Fetch object for fetching attribute values.
		final TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.NPuzzleView);

		try
		{
			// Set difficulty.
		    difficulty = attr.getInt(R.styleable.NPuzzleView_difficulty, difficulty);
		    
		    // Set count down seconds.
		    COUNTDOWN_SECONDS = attr.getInt(R.styleable.NPuzzleView_countdownSeconds, COUNTDOWN_SECONDS);
		    
		    // Set border drawable.
		    mBorder = attr.getDrawable(R.styleable.NPuzzleView_border);
		    
		    // Set image bitmap.
		    final BitmapDrawable src = (BitmapDrawable) attr.getDrawable(R.styleable.NPuzzleView_src);
		    
		    // Make sure the image isn't null.
		    if (src != null)
		    {
		    	setImage(src.getBitmap());
		    	// Start Game.
		    	init();
		    	mix(true);
		    }
		}
		finally
		{
			attr.recycle(); // Clean up.
		}
		
		init();
	}
	
	// ====================
	//    OnSolveListener
	// ====================
	
	public static interface OnSolveListener
	{
		public void onSolve();
	}
	
	// =====================
	//    Initialization
	// =====================
	
	private void init()
	{
		// Do not initialize twice.
		if (initialized) return;
		initialized = true;
		
		// Create Table Layout.
		mTableLayout = new TableLayout(mContext);
		this.addView(mTableLayout);
		
		// ====================
		//   Edit Count Down
		// TextView Styles Here.
		// ====================
		
		// Create and style Count Down Text.
		mCountdown = new TextView(mContext);
		mCountdown.setText(COUNTDOWN_SECONDS+"");
		mCountdown.setVisibility(TextView.VISIBLE);
		mCountdown.setGravity(Gravity.CENTER);
		mCountdown.setTextColor(Color.WHITE); // #FFFFFF
		mCountdown.setBackgroundColor(Color.argb(66, 33, 33, 33)); // #66333333
		mCountdown.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 60); // 60dip
		mCountdown.setPadding(16, 16, 16, 16); // 16dip
		this.addView(mCountdown);
		
		centerCountDown();
	}
	
	// Center mCountdown in this RelativeLayout.
	private void centerCountDown()
	{
		// Center mCountdown.
		final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mCountdown.getLayoutParams();
		layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, TRUE);
		mCountdown.setLayoutParams(layoutParams);
	}
	
	// Center mTableLayout in this RelativeLayout.
	private void centerTableLayout()
	{
		// Center mTableLayout.
		final RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mTableLayout.getLayoutParams();
		layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, TRUE);
		mTableLayout.setLayoutParams(layoutParams);
	}
	
	// ====================
	// Image Size Calculation
	// ====================
	
	/* Used to determine the maximum possible dimensions for an image
	 * based on the image's dimensions and those of its container (window) */
	private int[] imageDimensions(int imageWidth, int imageHeight)
	{
		int[] imageDims = new int[2],
			  maxDims = new int[2];
		float ratio = 0;
		
		// Get dimensions of layout.
		maxDims[0] = metrics.widthPixels;
		maxDims[1] = metrics.heightPixels;
				
		// 1:1 Aspect Ratio
		if (imageWidth == imageHeight)
		{
			// Set both height and width to the smaller window dimension
			imageDims[0] = imageDims[1] =
				(maxDims[0] > maxDims[1]) ? maxDims[1] : maxDims[0];
		}
		else
		{
	        // Check if the current width is larger than the max
	        if(imageWidth > maxDims[0])
	        {
	            ratio = (float) (maxDims[0] / (float) imageWidth);
	            imageWidth = maxDims[0];
	            imageHeight = (int) (imageHeight * ratio);
	        }
	
	        // Check if current height is larger than max
	        if(imageHeight > maxDims[1])
	        {
	            ratio = (float) (maxDims[1] / (float) imageHeight);
	            imageHeight = maxDims[1];
	            imageWidth = (int) (imageWidth * ratio);
	        }
	       	        
	        imageDims[0] = imageWidth;
	        imageDims[1] = imageHeight;
		}
		
	    return imageDims;
	}
	
	// Fetch the position corresponding
	// to the blank game piece.
	private int getBlankPosition()
	{
		final int blankIndex = (difficulty * difficulty) - 1;
		
		// Determine where the blank piece is.
		for (int i = 0; i < difficulty; i++)
		{
			for (int e = 0; e < difficulty; e++)
			{
				if (gameState[i][e] == blankIndex)
					return (i * difficulty) + e;
			}
		}
		
		return -1;
	}
	
	// Fetch the ImageView corresponding to
	// the blank game piece.
	private ImageView getBlankImageView()
	{
		final int blankPosition = getBlankPosition();
		
		// Find the blank ImageView.
		for (int i = 0, e = mTableLayout.getChildCount(); i < e; i++)
		{
			final ViewGroup rowView = (ViewGroup) mTableLayout.getChildAt(i);
			for (int j = 0, k = rowView.getChildCount(); j < k; j++)
			{
				final ImageView imgView = (ImageView) rowView.getChildAt(j);
				int imgPosition = (Integer) imgView.getTag();
				if (imgPosition == blankPosition)
					return imgView;
			}
		}
		
		return null;
	}
	
	// ====================
	//      Game Logic
	// ====================
	
	// Triggered when a puzzle piece is clicked.
	private OnClickListener pieceClicked = new OnClickListener()
	{
		@Override
		public void onClick(View view)
		{
			// Don't do anything if the game has been solved.
			if (isSolved || isCounting) return;
			
			// Initialize and Cast Views.
			ImageView imageView = (ImageView) view;			
			// Initialize integers.
			int position = (Integer) imageView.getTag(),
				row = (int)(position / difficulty),
				col = position % difficulty,
				direction = -1,
				blankPosition = getBlankPosition(),
				blankRow,
				blankCol;
			
			final ImageView blankView = getBlankImageView();
			
			blankCol = blankPosition % difficulty;
			blankRow = (int) (blankPosition / difficulty);
			
			// Check to see if the piece clicked is not the blank piece.
			if (position != blankPosition)
			{
				// Check right and left pieces.
				
				// Piece clicked is leftmost in its row.
				if (col == 0)
				{
					// Check the the piece to the right of the one clicked.
					if (position + 1 == blankPosition)
					{
						direction = 2;
					}
				}
				// Piece clicked is rightmost in its row.
				else if (col == difficulty - 1)
				{
					// Check the piece to the left of the one clicked.
					if (position - 1 == blankPosition)
					{
						direction = 3;
					}
				}
				// Piece clicked is in the middle of its row.
				else
				{
					// Check the piece to the left of the one clicked.
					if (position - 1 == blankPosition)
					{
						direction = 3;
					}
					// Check the the piece to the right of the one clicked.
					else if (position + 1 == blankPosition)
					{
						direction = 2;
					}
				}
				
				// Check top and bottom pieces.
				
				// Check the piece below the one clicked.
				if (position + difficulty == blankPosition)
				{
					direction = 1;
				}
				// Check the piece above the one clicked.
				else if (position - difficulty == blankPosition)
				{
					direction = 0;
				}
				
				// ====================
				// Here a piece adjacent
				// to the blank piece
				// has been clicked,
				// update styles/ move
				// pieces, animate, etc.
				// ====================
				
				// The piece clicked IS next to the blank piece.
				if (direction != -1)
				{
					++moves; // increment the number of moves.
					
					// Swap piece and blank piece in int array.
					int tmp = gameState[row][col];
					gameState[row][col] = gameState[blankRow][blankCol];
					gameState[blankRow][blankCol] = tmp;
					
					// Fetch image of clicked piece.
					final BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
					final Bitmap bitmap = drawable.getBitmap();
					
					// Swap piece and blank piece bitmaps.
					blankView.setImageBitmap(bitmap);
					blankView.setBackgroundDrawable(mBorder);
					
					imageView.setImageBitmap(mBlankBitmap);
					imageView.setBackgroundDrawable(null);
					
					// If the user has solved, notify listener.
					if (hasWon())
					{
						isSolved = true;
						onSolveListener.onSolve();
					}
				}
			}
		}
	};
	
	// Draws the table based on current state.
	// Fills mTableLayout with rows of
	// ImageViews, each set to one piece of
	// the image, then ClickListeners are
	// attached to each ImageViews.
	private void drawTable()
	{
		final Bitmap[] pieces = makePieces();
		
		// Removes all previous pieces.
		mTableLayout.removeAllViews();
						
		// Loop through N rows and N columns and make table.
		for (int i = 0; i < difficulty; ++i)
		{
			final TableRow row = new TableRow(mContext); // create a row.
			
			for (int e = 0; e < difficulty; ++e)
			{
				// Create an ImageView to add to the table.
				final ImageView img = new ImageView(mContext); // create an image.
				img.setImageBitmap(pieces[gameState[i][e]]); // set bitmap.
				img.setTag(new Integer((i * difficulty) + e));
				img.setOnClickListener(pieceClicked);
				img.setBackgroundDrawable(mBorder); // background border.
				row.addView(img); // add image to row.
			}
			
			mTableLayout.addView(row); // add row to table.
		}
		
		(getBlankImageView()).setBackgroundDrawable(null); // Remove border from blank piece.
		
		// Center mTableLayout & mCountdown.
		centerTableLayout();
		centerCountDown();
	}
	
	// Generates the array of Bitmap pieces
	// from the scaled version mScaledBitmap.
	private Bitmap[] makePieces()
	{
		// Get window metrics such as height, width, etc.
	    (((Activity) mContext).getWindowManager().getDefaultDisplay()).getMetrics(metrics);
		
		// Calculate image dimensions based on original ones.
		dims = imageDimensions(originalDims[0], originalDims[1]);
				
		// Initialize pieces.
		Bitmap[] pieces = new Bitmap[difficulty * difficulty];
				
		// Calculate the size of each piece.
	    pieceDims[0] = (int) dims[0] / difficulty;
	    pieceDims[1] = (int) dims[1] / difficulty;
	    
		// Generate a scaled bitmap.
    	final Bitmap mScaledBitmap = Bitmap.createScaledBitmap(mBitmap, dims[0], dims[1], true);
    	
    	// Initialize blank bitmap (no image required).
    	mBlankBitmap = Bitmap.createBitmap(pieceDims[0] - BORDER_WIDTH,
    			pieceDims[1] - BORDER_WIDTH, Bitmap.Config.ARGB_4444);
    	mBlankBitmap.eraseColor(Color.TRANSPARENT);

	    // Initialize some primitives.
		final int lastSquare = (difficulty * difficulty) -1;
		boolean isLastSquare;
		
		// Loop through every piece.
		for (int i = 0; i < difficulty; ++i)
		{
			for (int e = 0; e < difficulty; ++e)
			{
				isLastSquare = (lastSquare == (i * difficulty) + e);
				
				// If it is the last image, use a blank image (and cut from [0, 0]).
				// Create bitmap of game piece, subtract for border.
				pieces[(i*difficulty) + e] = Bitmap.createBitmap(
					((isLastSquare) ? mBlankBitmap : mScaledBitmap),
					((isLastSquare) ? 0 : pieceDims[0]*e),
					((isLastSquare) ? 0 : pieceDims[1]*i),
					pieceDims[0] - BORDER_WIDTH,
					pieceDims[1] - BORDER_WIDTH);
			}
		}
		
		mScaledBitmap.recycle(); // Clean up.
		
		return pieces;
	}
	
	// Runnable to deincrement the count of mCountdown
	// Called within a loop and set to run any number
	// of seconds so as to provide the illusion of
	// a count down.
	private Runnable deincrementCountdown =  new Runnable()
    {
    	@Override
    	public void run()
    	{
    		// Determine current number of seconds minus one.
    		final int seconds = (Integer.parseInt((String) mCountdown.getText())-1);
    		
    		// If we have reached zero seconds, hide
    		// the countdown and shuffle the puzzle.
    		if (seconds == 0)
    		{
    			// Hide countdown after COUNTDOWN_SECONDS
    			mCountdown.setVisibility(View.GONE);
    			isCounting = false; // Used to prevent clicks when counting.
    			shuffle(true); // Shuffles the puzzle.
    			drawTable();
    		}
    		else
    			// deincrement the countdown
    			mCountdown.setText(""+seconds);
    	}
    };
    
    private Runnable deincrementCountdownNoShuffle =  new Runnable()
    {
    	@Override
    	public void run()
    	{
			// Hide countdown after COUNTDOWN_SECONDS
			mCountdown.setVisibility(View.GONE);
			isCounting = false; // Used to prevent clicks when counting.
			drawTable();
    	}
    };
	
	// Counts down the TextView from `from` to Zero seconds,
	// then rehides it after `from` seconds.
	private void countDown()
	{
		 mCountdown.setVisibility(View.VISIBLE); // In case it was hidden.
		mCountdown.setText(""+COUNTDOWN_SECONDS);
	    final Handler mHandler = new Handler(); // Initialize handler.
	    
	    // Loop `from` to Zero seconds, counting down and displaying
	    // it to the user in a centered TextView
	    for (int i = 1; i <= COUNTDOWN_SECONDS; i++)
	    	// Set text to i seconds, i * 1000ms from now.
		    mHandler.postDelayed(deincrementCountdown, i * 1000);
	}
	
	// Shuffles gameState.
	private void shuffle(boolean mix)
	{
		gameState = new int[difficulty][difficulty];
		int i = 0;
		final int N = (difficulty * difficulty) -1;
			
		if (!mix)
		{
			while (i < N)
				gameState[(int)(i / difficulty)][i % difficulty] = ++i - 1;
			gameState[difficulty-1][difficulty-1] = N;
		}
		else
		{
			byte[] arr = getRandomArray();
			for (int j = 0, e = arr.length; j < e; j++)
				// Used to adapt to this application, whose pieces[] are
				// in ascending order and the last piece is blank.
				gameState[(int)(j / difficulty)][j % difficulty] = N - (int) arr[j];
		}
	}
	
	/*
	 * Credits are due to Brian Borowski.com for his random board shuffling.
	 * Source: http://www.brian-borowski.com/Software/Puzzle/.
	 * Inclusion of getRandomArray(), isValidPermutation(),
	 * and swap(final int i, final int j, final byte[] A).
	 */
	
	// Generate a random byte array of board state.
	private static byte[] getRandomArray()
	{
		final int numOfTiles = difficulty * difficulty;
		
        final byte[] tiles = new byte[numOfTiles];
        for (int i = numOfTiles - 2; i >= 0; --i)
            tiles[i] = (byte)(i + 1);
        
        tiles[numOfTiles - 1] = 0;

        int maxTilesToSwap;
        if (ZERO_IN_CORNER)
        {
        	maxTilesToSwap = numOfTiles - 1;
        }
        else
        {
        	maxTilesToSwap = numOfTiles;
        }
        
        final Random random = new Random();
        for (int i = 49; i >= 0; --i)
        {
            final int rand1 = random.nextInt(maxTilesToSwap);
            int rand2 = random.nextInt(maxTilesToSwap);
            if (rand1 == rand2)
            {
            	if (rand1 < (maxTilesToSwap << 1))
            	{
                    rand2 = random.nextInt(maxTilesToSwap - rand1) + rand1;
            	}
            	else
            	{
            		rand2 = random.nextInt(rand1);
            	}
            }
            swap(rand1, rand2, tiles);
        }
        
        if (!isValidPermutation(tiles))
        {
            if (tiles[0] != 0 && tiles[1] != 0)
            {
            	swap(0, 1, tiles);
        	}
        	else
        	{
        		swap(2, 3, tiles);
        	}
        }
                
        return tiles;
    }

	// Determine if byte array is a valid permutation.
    private static boolean isValidPermutation(final byte[] state)
    {
        final int numOfTiles = state.length,
                  dim = difficulty;
        int inversions = 0;

        for (int i = 0; i < numOfTiles; ++i)
        {
            final byte iTile = state[i];
            if (iTile != 0)
            {
                for (int j = i + 1; j < numOfTiles; ++j)
                {
                    final byte jTile = state[j];
                    if (jTile != 0 && jTile < iTile)
                    {
                    	++inversions;
                	}
                }
            }
            else
            {
                if ((dim & 0x1) == 0)
                {
                	inversions += (1 + i / dim);
                }
            }    
        }
        
        if ((inversions & 0x1) == 1) return false;
        return true;
    }
    
    // Swap two values in a byte array.
    private static void swap(final int i, final int j, final byte[] A)
    {
        final byte temp = A[i];
        A[i] = A[j];
        A[j] = temp;
    }
	
	// Check to see if the user has won the game.
	private boolean hasWon()
	{		
		for (int i = 0; i < difficulty; i++)
		{
			for (int e = 0; e < difficulty; e++)
			{
				if (gameState[i][e] != (i * difficulty) + e)
				{
					return false;
				}
			}
		}
			
		return true;
	}
	
	// ====================
	//       Clean Up
	// ====================
	
	@Override
	public void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();
		
		this.removeAllViews();
		
		// Recycle bitmaps.
		mBitmap.recycle();
		mBlankBitmap.recycle();
		metrics = null;
		mBorder = null;
	}
	
	// ====================
	//    Handle Resize
	// ====================
	
	/**
     * @see android.view.View#measure(int, int)
     */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		// Get window metrics such as height, width, etc.
	    (((Activity) mContext).getWindowManager().getDefaultDisplay()).getMetrics(metrics);
		
		// Calculate image dimensions based on original ones.
		dims = imageDimensions(originalDims[0], originalDims[1]);
		pieceDims[0] = (int) dims[0] / difficulty;
		pieceDims[1] = (int) dims[1] / difficulty;
		
		final int rowWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
				pieceDims[0] * difficulty, MeasureSpec.EXACTLY),
				tableHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
						pieceDims[1] * difficulty, MeasureSpec.EXACTLY),
		imgWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
						pieceDims[0], MeasureSpec.EXACTLY),
		imgHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
						pieceDims[1], MeasureSpec.EXACTLY);
		
		// Loop through every row in the table.
		final int count = mTableLayout.getChildCount();
		for (int i = 0; i < count; i++)
		{
			final ViewGroup row = (ViewGroup) mTableLayout.getChildAt(i);
			if (row.getVisibility() != View.GONE)
			{
				row.measure(rowWidthMeasureSpec, imgHeightMeasureSpec);
				// Loop through every ImageView in the row.
				final int rowCount = row.getChildCount();
				for (int e = 0; e < rowCount; e++)
				{
					final View img = row.getChildAt(e);
					if (img.getVisibility() != View.GONE)
					{
						img.measure(imgWidthMeasureSpec, imgHeightMeasureSpec);
					}
				}
			}
		}
		
		// Set the dimensions for this view & inner table layout.
		mTableLayout.measure(rowWidthMeasureSpec, tableHeightMeasureSpec);
		setMeasuredDimension(rowWidthMeasureSpec, tableHeightMeasureSpec);
	}
	
	// Handle when the layout needs to be resized.
	@Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh)
	{
		super.onSizeChanged(w, h, oldw, oldh);
		if (firstSizeChange)
		{
			firstSizeChange = false;
			return;
		}
		
		// BEWARE OF THIS HACK!!!
		// This is bad practice, but it works for now.
		// A better solution is certainly needed though.
		final Handler mHandler = new Handler();
		mHandler.postDelayed(deincrementCountdownNoShuffle, 10);
    }
}