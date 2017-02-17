package nl.dobots.bluenetexample;

import android.app.ProgressDialog;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONObject;

import nl.dobots.bluenet.ble.base.callbacks.IBooleanCallback;
import nl.dobots.bluenet.ble.base.callbacks.IDiscoveryCallback;
import nl.dobots.bluenet.ble.base.callbacks.IIntegerCallback;
import nl.dobots.bluenet.ble.base.callbacks.IProgressCallback;
import nl.dobots.bluenet.ble.base.callbacks.IStatusCallback;
import nl.dobots.bluenet.ble.base.structs.EncryptionKeys;
import nl.dobots.bluenet.ble.cfg.BleErrors;
import nl.dobots.bluenet.ble.cfg.BleTypes;
import nl.dobots.bluenet.ble.cfg.BluenetConfig;
import nl.dobots.bluenet.ble.extended.BleExt;
import nl.dobots.bluenet.ble.extended.CrownstoneSetup;
import nl.dobots.bluenet.utils.BleLog;

/**
 * This example activity shows the use of the bluenet library. The library is first initialized,
 * which enables the bluetooth adapter. It shows the following steps:
 *
 * 1. Connect to a device and discover the available services / characteristics
 * 2. Read a characteristic (PWM characteristic)
 * 3. Write a characteristic (PWM characteristic)
 * 4. Disconnect and close the device
 * 5. And how to do the 3 steps (connectDiscover, execute and disconnectClose) with one
 *    function call
 *
 * For an example of how to scan for devices see MainActivity.java or MainActivityService.java
 *
 * Created on 1-10-15
 * @author Dominik Egger
 */
public class ControlActivity extends AppCompatActivity {

	private static final String TAG = ControlActivity.class.getCanonicalName();

	private String _address;
	private BleExt _ble;
	private boolean _lightOn;

	private ImageView _lightBulb;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		initUI();

		_address = getIntent().getStringExtra("address");

		// create our access point to the library, and make sure it is initialized (if it
		// wasn't already)
		_ble = new BleExt();
		_ble.init(this, new IStatusCallback() {
			@Override
			public void onSuccess() {
				Log.v(TAG, "onSuccess");
			}

			@Override
			public void onError(int error) {
				Log.e(TAG, "onError: " + error);
			}
		});

		final ProgressDialog dlg = ProgressDialog.show(this, "Connecting", "Please wait...", true);

		// first we have to connect to the device and discover the available characteristics.
		if (Config.ENCRYPTION_ENABLED) {
			EncryptionKeys keys = new EncryptionKeys(Config.ADMIN_KEY, Config.MEMBER_KEY, Config.GUEST_KEY);
			_ble.getBleBase().setEncryptionKeys(keys);
			_ble.getBleBase().enableEncryption(true);
		}
		_ble.connectAndDiscover(_address, new IDiscoveryCallback() {
			@Override
			public void onDiscovery(String serviceUuid, String characteristicUuid) {
				// this function is called for every detected characteristic with the
				// characteristic's UUID and the UUID of the service it belongs.
				// you can keep track of what functions are available on the device,
				// but you don't have to, the library does that for you.
			}

			@Override
			public void onSuccess() {
				// once discovery is completed, this function will be called. we can now execute
				// the functions on the device. in this case, we want to know what the current
				// PWM state is

				// first we try and read the PWM value from the device. this call will make sure
				// that the PWM or State characteristic is available, otherwise an error is created
				_ble.readRelay(new IBooleanCallback() {
					@Override
					public void onSuccess(boolean result) {
						// if reading was successful, we get the value in the onSuccess as
						// the parameter

						// now we can update the image of the light bulb to on (if PWM value is
						// greater than 0) or off if it is 0
						updateLightBulb(result);

						// at the end we disconnect and close the device again. you could also
						// stay connected if you want. but it's preferable to only connect,
						// execute and disconnect, so that the device can continue advertising
						// again.
						_ble.disconnectAndClose(false, new IStatusCallback() {
							@Override
							public void onSuccess() {
								// at this point we successfully disconnected and closed
								// the device again
								dlg.dismiss();
							}

							@Override
							public void onError(int error) {
								// an error occurred while disconnecting
								dlg.dismiss();
							}
						});
					}

					@Override
					public void onError(int error) {
						// an error occurred while trying to read the PWM state
						Log.e(TAG, "Failed to get relay status: " + error);

						if (error == BleErrors.ERROR_CHARACTERISTIC_NOT_FOUND) {

							// return an error and exit if the PWM characteristic is not available
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(ControlActivity.this, "No relay characteristic found for this device!", Toast.LENGTH_LONG).show();
								}
							});
							dlg.dismiss();
							finish();
						} else {

							// disconnect and close the device again
							_ble.disconnectAndClose(false, new IStatusCallback() {
								@Override
								public void onSuccess() {
									// at this point we successfully disconnected and closed
									// the device again.
									dlg.dismiss();
								}

								@Override
								public void onError(int error) {
									// an error occurred while disconnecting
									dlg.dismiss();
								}
							});
						}
					}
				});
			}

			@Override
			public void onError(int error) {
				// an error occurred during connect/discover
				Log.e(TAG, "failed to connect/discover: " + error);
				dlg.dismiss();
				finish();
			}
		});

		/* You might think that was quite complicated for just reading the current PWM state.
		 *
		 * If you want to stay connected, then this is the way to go
		 *   1. connectAndDiscover
		 *   2. execute your functions
		 *   3. disconnectAndClose
		 *
		 * But if you just want to execute a function, without having to stay connected, the
		 * above 3 steps can be reduced to the following function:
		 *
		 * 	_ble.readPwm(_address, new IIntegerCallback() {
		 *		@Override
		 *		public void onSuccess(int result) {
		 *			updateLightBulb(result > 0);
		 *			dlg.dismiss();
		 *		}
		 *
		 *		@Override
		 *		public void onError(int error) {
		 *			Log.e(TAG, "Failed to get Pwm: " + error);
		 *			if (error == BleErrors.ERROR_CHARACTERISTIC_NOT_FOUND) {
		 *				runOnUiThread(new Runnable() {
		 *					@Override
		 *					public void run() {
		 *						Toast.makeText(ControlActivity.this, "No PWM Characteristic found for this device!", Toast.LENGTH_LONG).show();
		 *					}
		 *				});
		 *				dlg.dismiss();
		 *				finish();
		 *			}
		 *		}
		 *  });
		 *
		 * Each read or write function for a characteristic comes in two version.
		 *
		 * 1. A version without address parameter. To use this version, you need to be
		 *    connected already and have discovered the available services. Also after
		 *    executing the function, you will stay connected.
		 *
		 * 2. A version with an address parameter. The library will check if you are already
		 *    connected to the device, and if not it will first connect and discover, then
		 *    execute the function you called, and then trigger a timeout. If the timeout
		 *    expires, the library will automatically disconnect and close the device. If you call
		 *    another function within the timeout, the timeout will be restarted
		 **/

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// finish has to be called on the library to release the objects if the library
		// is not used anymore
		if (_ble.isConnected(null)) {
			_ble.disconnectAndClose(true, new IStatusCallback() {
				@Override
				public void onSuccess() {

				}

				@Override
				public void onError(int error) {

				}
			});
		}
		_ble.destroy();
	}

	private void initUI() {
		setContentView(R.layout.activity_control);

		_lightBulb = (ImageView) findViewById(R.id.imgLightBulb);
		_lightBulb.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				togglePower();
			}
		});

		Button btnPowerOn = (Button) findViewById(R.id.btnPowerOn);
		btnPowerOn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				powerOn();
			}
		});

		Button btnPowerOff = (Button) findViewById(R.id.btnPowerOff);
		btnPowerOff.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				powerOff();
			}
		});

		Button btnSetup = (Button) findViewById(R.id.btnSetup);
		btnSetup.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				executeSetup();
			}
		});

		Button btnReset = (Button) findViewById(R.id.btnReset);
		btnReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				factoryReset();
			}
		});
	}

	private void factoryReset() {
		final ProgressDialog dlg = ProgressDialog.show(this, "Executing Factory Reset", "Please wait...", true);
		_ble.writeFactoryReset(_address, new IStatusCallback() {
			@Override
			public void onSuccess() {
				Log.d(TAG, "successfully reset to factory settings");
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(ControlActivity.this, "success", Toast.LENGTH_LONG).show();
					}
				});
				dlg.dismiss();
			}

			@Override
			public void onError(final int error) {
				Log.e(TAG, "failed to reset to factory");

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(ControlActivity.this, "failed with error: " + error, Toast.LENGTH_LONG).show();
					}
				});
				dlg.dismiss();
			}
		});
	}

	private void executeSetup() {
		final ProgressDialog dlg = new ProgressDialog(this);
		dlg.setTitle("Executing Setup");
		dlg.setMessage("Please wait ...");
		dlg.setIndeterminate(false);
		dlg.setMax(13);
		dlg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dlg.show();

		CrownstoneSetup setup = new CrownstoneSetup(_ble);
		_ble.enableEncryption(true);
		setup.executeSetup(_address,
			1,
			Config.ADMIN_KEY,
			Config.MEMBER_KEY,
			Config.GUEST_KEY,
			0x9449d07c,
			"b1a64158-ada0-4cc1-b508-eab42c82cd81",
			123,
			456,
			new IProgressCallback() {

				@Override
				public void onError(final int error) {
					BleLog.getInstance().LOGe(TAG, "failed with error: %d", error);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(ControlActivity.this, "failed with error: " + error, Toast.LENGTH_LONG).show();
						}
					});
				}

				@Override
				public void onProgress(final double progress, @Nullable JSONObject statusJson) {
					BleLog.getInstance().LOGi(TAG, "progress: %f", progress);

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							dlg.setProgress((int) progress);
						}
					});
				}
			}, new IStatusCallback() {

				@Override
				public void onError(final int error) {
					BleLog.getInstance().LOGe(TAG, "status error: %d", error);

					dlg.dismiss();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(ControlActivity.this, "status error: " + error, Toast.LENGTH_LONG).show();
						}
					});
				}

				@Override
				public void onSuccess() {
					BleLog.getInstance().LOGd(TAG, "success");

					dlg.dismiss();

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(ControlActivity.this, "success", Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		);
	}

	private void powerOff() {
		// switch the device off. this function will check first if the device is connected
		// (and connect if it is not), then it switches the device off, and disconnects again
		// afterwards (once the disconnect timeout expires)
		_ble.relayOff(_address, new IStatusCallback() {
			@Override
			public void onSuccess() {
				Log.i(TAG, "power off success");
				// power was switch off successfully, update the light bulb
				updateLightBulb(false);
			}

			@Override
			public void onError(int error) {
				Log.i(TAG, "power off failed: " + error);
			}
		});
	}

	private void powerOn() {
		// switch the device on. this function will check first if the device is connected
		// (and connect if it is not), then it switches the device on, and disconnects again
		// afterwards (once the disconnect timeout expires)
		_ble.relayOn(_address, new IStatusCallback() {
			@Override
			public void onSuccess() {
				Log.i(TAG, "power on success");
				// power was switch on successfully, update the light bulb
				updateLightBulb(true);
			}

			@Override
			public void onError(int error) {
				Log.i(TAG, "power on failed: " + error);
			}
		});
	}

	private void togglePower() {
		// toggle the device switch, without needing to know the current state. this function will
		// check first if the device is connected (and connect if it is not), then it reads the
		// current relay state, and depending on the state, decides if it needs to switch it on or
		// off. in the end it disconnects again (once the disconnect timeout expires)
		_ble.toggleRelay(_address, new IBooleanCallback() {
			@Override
			public void onSuccess(boolean value) {
				Log.i(TAG, "toggle success");
				// power was toggled successfully, update the light bulb
				updateLightBulb(value);
			}

			@Override
			public void onError(int error) {
				Log.e(TAG, "toggle failed: " + error);
			}
		});
	}

	private void updateLightBulb(final boolean on) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				_lightOn = on;
				if (on) {
					_lightBulb.setImageResource(getResources().getIdentifier("light_bulb_on", "drawable", getPackageName()));
				} else {
					_lightBulb.setImageResource(getResources().getIdentifier("light_bulb_off", "drawable", getPackageName()));
				}
			}
		});
	}

}
