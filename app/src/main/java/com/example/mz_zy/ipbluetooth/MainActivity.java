package com.example.mz_zy.ipbluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView mStatusTv;
    private Button mConnBtn;

    private ProgressDialog mProgressDlg;

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    TextView mNameTv;
    TextView mBTAddressTv;
    TextView mRemoteIPTv;
    TextView mLocalIPTv;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNameTv = (TextView) findViewById(R.id.tv_name_value);
        mBTAddressTv = (TextView) findViewById(R.id.tv_btaddr_value);
        mRemoteIPTv = (TextView) findViewById(R.id.tv_remoteaddr_value);
        mLocalIPTv = (TextView) findViewById(R.id.tv_localaddr_value);

        mConnBtn = (Button) findViewById(R.id.ipconn);

        NetworkInterface btface = null;
        try {
            btface = NetworkInterface.getByName("bt-pan");
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }

        if(btface == null)
        {
            showToast("No Device Connected");
            return;
        }
        String locIP = getIP(btface);
        mLocalIPTv.setText(locIP);

        BluetoothDevice devBT = getDevice();
        if(devBT != null)
        {
            mBTAddressTv.setText(devBT.getAddress());
            mNameTv.setText(devBT.getName());
            String deviceaddr = devBT.getAddress();
            Method m = null;
            try {
                m = devBT.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            BluetoothSocket btSocket = null;
            try {
                btSocket = (BluetoothSocket) m.invoke(devBT, 1);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            try {
                btSocket.connect();
                InputStream inStream = btSocket.getInputStream();
                OutputStream outStream = btSocket.getOutputStream();
                String message = "Hello from Android.\n";
                byte[] msgBuffer = message.getBytes();
                outStream.write(msgBuffer);
                byte[] incoming = new byte[1024];
                int ret = inStream.read(incoming);
                StringBuilder sb = new StringBuilder(ret);
                for (int i = 0; i < ret; ++ i) {
                    if (incoming[i] < 0) throw new IllegalArgumentException();
                    sb.append((char) incoming[i]);
                }
                String ipresult = sb.toString();
                System.out.println(ipresult);
                ProcessBuilder writeIP = new ProcessBuilder("su","-c echo \"" + ipresult + "\" > /mnt/internal_sd/ip.txt");
                Process execWriteIP = writeIP.start();
                try {
                    execWriteIP.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mRemoteIPTv.setText(ipresult);
                inStream.close();
                outStream.close();
                btSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }



        mConnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    NetworkInterface btface = NetworkInterface.getByName("bt-pan");
                    String locIP = getIP(btface);
                    if (locIP.equals("")) {
                        try {
                            try {
                                ProcessBuilder processBuilder = new ProcessBuilder("su", "-c ifconfig bt-pan 192.168.13.2");
                                Process process = processBuilder.start();
                                process.waitFor();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            NetworkInterface btfacenew = NetworkInterface.getByName("bt-pan");
                            locIP = getIP(btfacenew);
                            mLocalIPTv.setText(locIP);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        showToast("Already Set");
                        return;
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            }
        });



    }

    private String getIP(NetworkInterface btface) {
        String ipAddress = "";
        for (Enumeration enumIpAddr = btface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
            InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
            if (!inetAddress.isLoopbackAddress()&&inetAddress instanceof Inet4Address) {
                ipAddress=inetAddress.getHostAddress().toString();
                break;
            }
        }
        return ipAddress;
    }


    private BluetoothDevice getDevice(){
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        ArrayList<BluetoothDevice> list = new ArrayList<BluetoothDevice>();
        list.addAll(pairedDevices);
        Iterator it = list.iterator();
        while(it.hasNext()){
            BluetoothDevice device = (BluetoothDevice) it.next();
            Method m = null;
            try {
                m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            try {
                BluetoothSocket btSocket = (BluetoothSocket) m.invoke(device, 1);
                if(btSocket != null){
                    return device;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        }
        return null;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

}
