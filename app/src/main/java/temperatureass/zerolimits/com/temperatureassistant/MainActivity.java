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
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;

public class MainActivity extends ActionBarActivity {
    public static final String NEWLINE = "\r\n";
    public static final int PORT = 1900;
    public static final String HOST = "Host: 239.225.255.250:1900";
    public static final String ADDRESS = "239.225.255.250";
    public static final String ST = "ST: ";
   // public static final String LOCATION = "LOCATION";
   // public static final String NT = "NT";
   // public static final String NTS = "NTS";
   // public static final String SL_NOTIFY = "NOTIFY * HTTP/1.1";
    public static final String SL_MSEARCH = "M-SEARCH * HTTP/1.1";
   // public static final String SL_OK = "HTTP/1.1 200 OK";
    public static final String ST_ContentDirectory = "schemas-upnp-org:service:TemperatureSensor:1";
    public static final String USER_AGENT = "User-Agent: stuff";
    public static final String CONNECTION = "Connection: close";

    // public static final String NTS_ALIVE = "ssdp:alive";
    //  public static final String NTS_BYEBYE = "ssdp:byebye";
    //  public static final String NTS_UPDATE = "ssdp:update";
    public static final String NTS_DISCOVER = "MAN: \"ssdp:discover\"";
    public static final String MX = "MX: 5";
    public static final String TAG = "temp";
  //  public static final String GRAPH_URL = "/graph.png";

    private Button id;
    private Button graph;
    private EditText info;
    private WebView tempGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);

        id = (Button) findViewById(R.id.idBtn);
        graph = (Button) findViewById(R.id.grphBtn);
        Button getTemp = (Button) findViewById(R.id.crntTemp);
        info = (EditText) findViewById(R.id.dev_info);

        id.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

               DeviceFinder dfTask = new DeviceFinder();
               DeviceListener dlTask = new DeviceListener();

              dfTask.execute();
              dlTask.execute();
            }
        });

        getTemp.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                RequestQueue que = Volley.newRequestQueue(MainActivity.this);
                String url = "";
                String message = "stuff";

                StringRequest sr = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                    @Override
                public void onResponse(String response){
                        //todo Parse response for temperature

                    }
                }, new Response.ErrorListener() {
                    @Override
                   public void onErrorResponse(VolleyError error){

                    }
                });
                que.add(sr);

                //todo Setup code for sending query to the server for temperature
                openAlert(v, message);
            }
        });

        graph.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v){
                tempGraph = (WebView) findViewById(R.id.tempGrph);
                if(info.getText().toString() == "") {
                    Log.d(TAG, "Need to enter an IP address");
                    Toast.makeText(MainActivity.this, "Please enter an IP address", Toast.LENGTH_LONG).show();
                    return;
                }
                    tempGraph.setVisibility(View.VISIBLE);
                    tempGraph.loadUrl("http://www.ampyourstrat.com/wp-content/uploads/android-logo.jpg");
            }
        });
    }

    private void openAlert(View view, String temperature) {
        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle(R.string.alert_dialog_title);
        alertBuilder.setMessage(temperature);
        alertBuilder.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface di, int id){
                    di.dismiss();
            }
        });
        AlertDialog ad = alertBuilder.create();
        ad.show();
    }

    class DeviceListener extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute(){
            info.setText("Looking for response from temperature device.");
        }

        @Override
        protected String doInBackground(Void... params) {
            Log.d(TAG, "Execute");
            byte[] buf = new byte[1024];
            try {
                MulticastSocket clientMultiSock = new MulticastSocket(PORT);
                clientMultiSock.joinGroup(InetAddress.getByName(ADDRESS));
                Log.d(TAG, "Setup multicast socket");
                while (true){
                    DatagramPacket recv = new DatagramPacket(buf, 0, buf.length);
                    clientMultiSock.receive(recv);

                    final String msg = new String(recv.getData());
                    Log.d(TAG, msg);
                    if( msg.contains("M-SEARCH")){
                        final String clientAddress = recv.getAddress().getHostAddress();
                        clientMultiSock.disconnect();
                        return clientAddress;
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Could not send the packet");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String address){
            info.setText(address);
        }
    }

    class DeviceFinder extends AsyncTask<Void,Void,Void>
    {
        @Override
        protected void onPreExecute(){
            info.setText("Contacting temperature device");
        }

        @Override
        protected Void doInBackground(Void... params){
            MulticastSocket msock;
            String query = SL_MSEARCH + NEWLINE + MX + NEWLINE + ST + ST_ContentDirectory + NEWLINE + NTS_DISCOVER + NEWLINE + USER_AGENT + NEWLINE + CONNECTION + NEWLINE + HOST + NEWLINE + NEWLINE;
            try {

                msock = new MulticastSocket(PORT);
                msock.setTimeToLive(5);
                msock.joinGroup(InetAddress.getByName(ADDRESS));
                msock.send(new DatagramPacket(query.getBytes(), query.getBytes().length, InetAddress.getByName(ADDRESS), PORT));
                msock.setTimeToLive(5);
                Log.d(TAG, "Sent the packet");
                msock.leaveGroup(InetAddress.getByName(ADDRESS));

            } catch (IOException e1) {
                Log.e(TAG, "There was an error sending the packet");
                e1.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){

        }
    }
}


