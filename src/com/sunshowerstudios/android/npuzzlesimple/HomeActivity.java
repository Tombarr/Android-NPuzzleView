package com.sunshowerstudios.android.npuzzlesimple;

import com.sunshowerstudios.android.npuzzlesimple.NPuzzleView;
import com.sunshowerstudios.android.npuzzlesimple.NPuzzleView.OnSolveListener;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

// This is a demo app of NPuzzleView.

public class HomeActivity extends Activity implements OnSolveListener
{
    private NPuzzleView mGame;
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
                
        // Fetch NPuzzleView.
        mGame = (NPuzzleView) findViewById(R.id.game_board);
        
        mGame.setOnSolveListener(this); // Attach onSolveListener.
        
        // Other examples include :
        // mGame.reload();
        // mGame.setDifficulty();
        // mGame.setImageUri(Uri uri);...
    }
    
	@Override
	public void onSolve()
	{
		// Show a Toast telling the user how many moves they made.
		Toast.makeText(getApplicationContext(), "You Won With " + mGame.getMoves() + " Moves!", Toast.LENGTH_SHORT).show();
	}
}