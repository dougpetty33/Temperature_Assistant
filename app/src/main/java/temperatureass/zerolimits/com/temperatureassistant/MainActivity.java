package temperatureass.zerolimits.com.temperatureassistant;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends ActionBarActivity {
    public static final String NEWLINE = "\r\n";
    public static final int PORT = 1900;
    public static final String HOST = "Host: 239.255.255.250:1900";
    public static final String ADDRESS = "239.255.255.250";
    public static final String ST = "ST: ";
    // public static final String SL_NOTIFY = "NOTIFY * HTTP/1.1";
    public static final String SL_MSEARCH = "M-SEARCH * HTTP/1.1";
    public static final String SL_OK = "HTTP/1.1 200 OK";
    public static final String ST_ContentDirectory = "zerolimits:sprinkler";
    public static final String USER_AGENT = "User-Agent: stuff";
    public static final String CONNECTION = "Connection: close";
    public static final String NTS_DISCOVER = "MAN: \"ssdp:discover\"";
    public static final String MX = "MX: 5";
    public static final String TAG = "temp";

    private EditText info;
    private WebView tempGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);
        Button id;
        Button graph;

        id = (Button) findViewById(R.id.idBtn);
        graph = (Button) findViewById(R.id.grphBtn);
        Button getTemp = (Button) findViewById(R.id.crntTemp);
        info = (EditText) findViewById(R.id.dev_info);

        Thread t = new Thread(new Listener());
        t.start();

        id.setOnClickListener(new View.OnClickListener() {

            @SuppressWarnings("UnusedAssignment")
            public void onClick(View v) {

                sendPacket sp = new sendPacket();
                sp.execute();
                sp = null;
                Log.d(TAG, "ID was pressed");
            }
        });

        getTemp.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                RequestQueue que = Volley.newRequestQueue(MainActivity.this);
                String url = "http://192.168.2.123/getTemp?units=c";
                String message = "stuff";

                StringRequest sr = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //todo Parse response for temperature
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
                que.add(sr);

                //todo Setup code for sending query to the server for temperature
                openAlert(message);
            }
        });


        graph.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                tempGraph = (WebView) findViewById(R.id.tempGrph);
                if (info.getText().toString().equals("")) {
                    Log.d(TAG, "Need to enter an IP address");
                    Toast.makeText(MainActivity.this, "Please enter an IP address", Toast.LENGTH_LONG).show();
                } else {
                    tempGraph.setVisibility(View.VISIBLE);
                    tempGraph.loadUrl("http://www.ampyourstrat.com/wp-content/uploads/android-logo.jpg");
                }
            }
        });
    }

    private void openAlert(String temperature) {
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle(R.string.alert_dialog_title);
        alertBuilder.setMessage(temperature);
        alertBuilder.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface di, int id) {
                di.dismiss();
            }
        });
        AlertDialog ad = alertBuilder.create();
        ad.show();
    }

    class Listener implements Runnable {
        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];
                DatagramSocket sock = new DatagramSocket(PORT);
                final DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    sock.receive(incoming);
                    Log.d(TAG, "Receive from " + incoming.getAddress().getHostAddress());
                    String response = new String(incoming.getData());
                    if (response.contains(MainActivity.SL_OK) && response.contains(MainActivity.ST_ContentDirectory)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.info.setText(incoming.getAddress().getHostAddress());
                            }
                        });
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
                Log.e(TAG, "Socket Exception");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IO Exception");
            }
        }
    }

    class sendPacket extends AsyncTask<Void, Void, Void> {
        private String search = SL_MSEARCH + NEWLINE + MX + NEWLINE + ST + ST_ContentDirectory + NEWLINE + NTS_DISCOVER + NEWLINE + USER_AGENT + NEWLINE + CONNECTION + NEWLINE + HOST + NEWLINE + NEWLINE;

        @Override
        protected void onPreExecute() {
            MainActivity.this.info.setText("Looking for device..... ");
        }

        @SuppressWarnings("UnusedAssignment")
        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.d(TAG, "Generating send packet");
                DatagramSocket ds = new DatagramSocket();
                DatagramPacket sendPacket = new DatagramPacket(search.getBytes(), search.getBytes().length, InetAddress.getByName(ADDRESS), PORT);
                ds.send(sendPacket);
                ds.close();
                ds = null;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (SocketException e) {
                e.printStackTrace();
                Log.e(TAG, "Socket Exception");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "IO Exception");
            }
            return null;
        }


    }
}




