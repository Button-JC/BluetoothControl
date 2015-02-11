package net.lumportantoj.bluetoothcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
//import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass.Device.Major;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
//import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class BluetoothControlActivity extends Activity {
    /** Called when the activity is first created. */
	protected BluetoothAdapter mBluetoothAdapter;
	private Handler handler = new Handler();
	MessagingThread messThread;
	private int REQUEST_ENABLE_BT = 1;
	private ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
    private Button buttonWhatsWrong;
    private Toast toast;

	@Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        this.toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT); //init toast for later usage
        buttonWhatsWrong = (Button) findViewById(R.id.whatsWrongButt);
        buttonWhatsWrong.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onCreate(savedInstanceState);
            }
        });
         
        /* Let's check if the device has BT module */
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
        	builder.setMessage("BlueTooth not supported by this device. Exit?")
        	       .setCancelable(false)
        	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	        	   BluetoothControlActivity.this.finish();
        	           }
        	       })
        	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
        	           public void onClick(DialogInterface dialog, int id) {
        	                dialog.cancel();
        	           }
        	       });
        	AlertDialog alert = builder.create();
        	alert.show();
        	//Toast.makeText(getApplicationContext(), "Bluetooth nerozpozn�n", 1000).show();
        }
        
        /* Check if BT is enabled */
        if (!mBluetoothAdapter.isEnabled()) {
        	// request to turn on BT
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);  
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT );
        }else{
        	 getDevices(); // get list of devices
        }
	}

    /**
     * Triggered on event return
     * @param requestCode origin request code
     * @param resultCode result code
     * @param data additional data
     */
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data){
        if (requestCode==REQUEST_ENABLE_BT){
            //Toast.makeText(getApplicationContext(), "BT activity", 1000).show();
            if (resultCode==RESULT_OK){
                getDevices();
            }else if (resultCode==RESULT_CANCELED){
                btEnableDialog();
            }
        }
        //Toast.makeText(getApplicationContext(), "activity req code: "+requestCode+"res code: "+resultCode, 1000).show();
    }

    /**
     * Generates dialog to prompt user to turn on BT
     */
	private void btEnableDialog(){
		AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
    	builder2.setMessage("Can't do sh** without bluetooth. Try again?")
    	       .setCancelable(false)
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    	   				startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT );
    	           }
    	       })
    	       .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   BluetoothControlActivity.this.finish();
    	           }
    	       });
    	AlertDialog alert2 = builder2.create();
    	alert2.show();
    	/********/
    	//Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		//startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT );
	}

    /**
     * Get list of paired BT devices and prompt user to select one
     */
	void getDevices(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
        	CharSequence[] names= new String[pairedDevices.size()];
        	// Loop through paired devices
        	int c =0;
	        for (BluetoothDevice device : pairedDevices) {
		        // Add the name and address to an array adapter to show in a ListView
		        devices.add(device);
		        if(device.getBluetoothClass().getMajorDeviceClass()==Major.TOY){
		        	names[c++]=device.getName()+" (Toy) ";
		        }else{
		        	names[c++]=device.getName()+" (not a Toy) ";
		        }
		        
	        }
	        AlertDialog.Builder builder = new AlertDialog.Builder(this);
	        builder.setTitle("Pick a device");
	        builder.setItems(names, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int item) {
	                //Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
	            	connect(item);
	            }
	        });
	        AlertDialog alert2 = builder.create();
	        alert2.show();
	      //  Toast.makeText(getApplicationContext(),"alert!!!!!!!!", Toast.LENGTH_SHORT).show();
        	
	        
        }
	}

    /**
     * Connect to selected device
     * @param deviceNum device ID number
     */
	void connect(int deviceNum){

        ConnectThread t = new ConnectThread(devices.get(deviceNum));
        toasting("connecting : "+devices.get(deviceNum).getAddress());
        t.start();
        
        View arrows = findViewById(R.id.ivArrows);
        //buttF.setVisibility(buttF.VISIBLE);

        /* set events on touches */
        arrows.setOnTouchListener(new View.OnTouchListener() {		
			public boolean onTouch(View v, MotionEvent event) {
				char   f,b,r,l;
				f=b=l=r=0;
				byte[] byteMsg=new byte[1];
				if(event.getAction() == MotionEvent.ACTION_MOVE) {
					//Toast.makeText(getApplicationContext(), "x:"+event.getX()+" y:"+event.getY(), 0).show();
					
					if(event.getX()<45){ // left
						l=1;
					}else if(event.getX()>170){ // right
						r=1;
					}
					if(event.getY()<160){  // forward
						f=1;
					}else if(event.getY()>330){ // back
						b=1;
					} 
					byteMsg[0]=(byte)((f+2*b+4*l+8*r)& 0xFF); // code direction
					toasting("x:"+event.getX()+" y:"+event.getY()+"  msg: "+byteMsg[0]);
					try {
						messThread.write(byteMsg);
					} catch (Exception e) {
						toasting("sending error");
					}
					 
		        } else if (event.getAction() == MotionEvent.ACTION_UP) {
		        	//messThread.write("2".getBytes()); 
		        	byteMsg[0]=(byte)0x00;
		        	try {
						messThread.write(byteMsg);
					} catch (Exception e) {
						toasting("sending error");
					}
		        }
				return true;
			}
        });
        buttonWhatsWrong.setVisibility(View.INVISIBLE);
	}

    /**
     * Helper method for fast toast texts
     * @param s text to be toasted
     */
	public void toasting(CharSequence s){
		this.toast.setText(s);
		this.toast.show();
	}

    /**
     * Bluetooth connection thread
     */
	private class ConnectThread extends Thread {
	    private BluetoothSocket mmSocket;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        
	        Method m;
	        try {
				m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
				mmSocket =(BluetoothSocket)m.invoke(device, 1);
	        } catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
	        /*
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("00a95ac0-6d22-11e1-b0c4-0800200c9a66"));
	        } catch (IOException e) { }
	        */
	        
	    }
	 
	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        mBluetoothAdapter.cancelDiscovery();
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	        } catch (IOException connectException) {
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException ignored) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        handler.post(new Runnable() {
							public void run() {
								messThread = new MessagingThread(mmSocket);
								toasting("messaging thread started");
								messThread.start();
							}
						});
	        
	    }
	 
	    /** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException ignored) { }
	    }
	}

    /**
     * Bluetooth messaging thread
     */
	private class MessagingThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()

	    
	    public MessagingThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException ignored) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 

		public void run() {
	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                // Send the obtained bytes to the UI activity
	    	        handler.post(new Runnable() {
	    							public void run() {
	    								toasting( new String(buffer,0,bytes));
	    							}
	    	        });

	            } catch (IOException e) {
	                break;
	            }
	        }
	    }

		/* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException ignored) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException ignored) { }
	    }
	}
	
	
}