package edu.cmu.cs.speech.tts.flite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class FliteManager extends Activity {
	private final static String LOG_TAG = "Flite_Java_" + FliteManager.class.getSimpleName();	


	private ProgressDialog mProgress;
	private final static String FLITE_DATA_PATH = CheckVoiceData.getDataPath();
	private TTS tts;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.flitemanager);
		
		Button b = (Button) findViewById(R.id.button1);

		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				syncVoiceList();
			}
		});
		
		Button bInfo = (Button) findViewById(R.id.button_info);
		bInfo.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent openInfo = new Intent(FliteManager.this, FliteInfoViewer.class);
				startActivity(openInfo);
				
			}
		});
		
		Button bPlay = (Button) findViewById(R.id.button_play);
		bPlay.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tts = new TTS(FliteManager.this);				
			}
		});
			
			
	}
	private class TTS implements OnInitListener {
		TextToSpeech tts;
		
		TTS(Context c) {
			tts = new TextToSpeech(c, this);
			
		}
		
		public void onInit(int status) {
			tts.setEngineByPackageName("edu.cmu.cs.speech.tts.flite");
			say("This is an example sentence spoken by the flite speech synthesizer");
		}
		
		public void say(String text) {
			tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		}
	}
	
	private boolean syncVoiceList() {
		/*
		 * Connect to CMU TTS server and get the list of voices available
		 */
		final String voiceListURL = "http://tts.speech.cs.cmu.edu/android/vox-flite-1.5.6/voices.list?q=2";
		final String voiceListFile = FLITE_DATA_PATH + "cg/voices.list";

		final FileDownloader fdload = new FileDownloader();

		mProgress = new ProgressDialog(this);
		mProgress.setCancelable(true);
		mProgress.setIndeterminate(true);
		mProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgress.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel",
				new ProgressDialog.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				fdload.abort();
			}
		});
		mProgress.show();

		final Builder syncSuccessStatus = new AlertDialog.Builder(this);
		syncSuccessStatus.setPositiveButton("Ok", null);

		new Thread() {
			public void run() {
				fdload.saveUrlAsFile(voiceListURL, voiceListFile);
				while (fdload.totalFileLength == 0) {
					if (fdload.finished)
						break;
				}
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						if (fdload.totalFileLength > 0) {
							mProgress.setIndeterminate(false);
							mProgress.setMax(fdload.totalFileLength);
						}
					}
				});
				int prev = 0;
				while (!fdload.finished) {
					if (fdload.totalFileLength == -1)
						continue;
					if (fdload.finishedFileLength - prev > 10000) {
						prev = fdload.finishedFileLength;
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								mProgress
								.setProgress(fdload.finishedFileLength);
							}
						});
					}
				}
				runOnUiThread(new Runnable() {

					@Override
					public void run() {

						mProgress.dismiss();

					}
				});
				if (!fdload.success) {
					Log.e(LOG_TAG, "Voice list download failed!");
					if (fdload.abortDownload)
						syncSuccessStatus.setMessage("Live Update aborted.");
					else
						syncSuccessStatus
						.setMessage("Live Update failed! Check your internet settings.");
				} else {
					syncSuccessStatus
					.setMessage("Live update succesful. Voice list synced with server.");
				}
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						syncSuccessStatus.show();
					}
				});
			}
		}.start();
		return true;
	}

}