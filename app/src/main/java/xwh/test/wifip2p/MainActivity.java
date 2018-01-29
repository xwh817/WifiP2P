package xwh.test.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;

public class MainActivity extends AppCompatActivity {


	private static final String TAG = "WiFiDirectReceiver";
	private WifiP2pManager mManager;
	private WifiP2pManager.Channel mChannel;

	private IntentFilter directFilter;
	private BroadcastReceiver directReceiver;

	private TextView mTextInfo;
	private Button mButtonDiscover;
	private ListView mListView;
	private DeviceListAdapter mListAdapter;

	private boolean p2pEnable = false;

	private Collection<WifiP2pDevice> mWifiP2pDevices;

	private boolean isService = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mButtonDiscover = this.findViewById(R.id.bt_discover);
		mTextInfo = this.findViewById(R.id.text_info);
		mListView = this.findViewById(R.id.list_devices);
		mListAdapter = new DeviceListAdapter(this);
		mListView.setAdapter(mListAdapter);

		mButtonDiscover.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (p2pEnable) {
					discoverPeers();
				} else {
					mTextInfo.append("WifiP2P is disable\n");

					startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
				}

			}
		});

		findViewById(R.id.bt_discover_stop).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				stopDiscoverPeers();
			}
		});

		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				WifiP2pDevice device = mListAdapter.getItem(position);
				connectDevice(device);
			}
		});

		this.findViewById(R.id.bt_send).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isService) {
					sendMessage(mResponseClient, "I am from Service" +System.currentTimeMillis());
				} else {
					sendMessage(clientSocket, "I am client " +System.currentTimeMillis());
				}
			}
		});

		mManager = (WifiP2pManager) this.getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(this, this.getMainLooper(), null);
		directReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				Log.e(TAG, "===============wifi direct action: " + action);
				if (action.equals(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)) {
					int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
					if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
						//打开
						mTextInfo.append("WIFI_P2P_STATE_ENABLED\n");
						p2pEnable = true;
					} else if (state == WifiP2pManager.WIFI_P2P_STATE_DISABLED) {
						//关闭
						mTextInfo.append("WIFI_P2P_STATE_DISABLED\n");
						p2pEnable = false;
					}
				} else if (action.equals(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)) {  // 可用设备列表发生变化
					mTextInfo.append("WIFI_P2P_PEERS_CHANGED_ACTION\n");
					requestPeers();
				} else if (action.equals(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)) {     // 连接状态发生变化

					NetworkInfo info = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
					if (info.isConnected()) {
						getConnectionInfo();
					}
					mTextInfo.append("WIFI_P2P_CONNECTION_CHANGED_ACTION\n  isConnected:" + info.isConnected()+"\n");

				} else if (action.equals(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)) {

				}
			}
		};

		directFilter = new IntentFilter();
		directFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		directFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		directFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		directFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

	}

	//注册广播监听
	@Override
	protected void onResume() {
		super.onResume();
		this.registerReceiver(directReceiver, directFilter);
	}

	//取消注册
	@Override
	protected void onPause() {
		super.onPause();
		this.unregisterReceiver(directReceiver);
	}


	private void discoverPeers() {
		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.e(TAG, "===================discovery success");
				mTextInfo.append("\nDiscovery success\n");
			}

			@Override
			public void onFailure(int reason) {
				Log.e(TAG, "===================discovery failed");
				mTextInfo.append("Discovery failed\n");
			}
		});

	}


	private void stopDiscoverPeers() {
		mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				Log.e(TAG, "===================stopPeerDiscovery success");
				mTextInfo.append("\nstopPeerDiscovery success\n");
			}

			@Override
			public void onFailure(int reason) {
				Log.e(TAG, "===================stopPeerDiscovery failed");
				mTextInfo.append("stopPeerDiscovery failed\n");
			}
		});

	}


	private void requestPeers() {
		if (null != mManager) {
			mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
				@Override
				public void onPeersAvailable(WifiP2pDeviceList peers) {

					mWifiP2pDevices = peers.getDeviceList();
					Log.e(TAG, "==================peers list size: " + mWifiP2pDevices.size());

					if (mWifiP2pDevices.size() != mListAdapter.getCount()) {
						ArrayList<WifiP2pDevice> deviceList = new ArrayList<>();
						deviceList.addAll(mWifiP2pDevices);
						mListAdapter.setData(deviceList);
					}

				}

			});
		}

	}


	private void connectDevice(WifiP2pDevice device) {
		WifiP2pConfig config = new WifiP2pConfig();
		config.deviceAddress = device.deviceAddress;

		mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
			@Override
			public void onSuccess() {
				mTextInfo.append("createGroup onSuccess\n");
			}

			@Override
			public void onFailure(int reason) {
				mTextInfo.append("createGroup failed\n");
			}
		});


		mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

			@Override
			public void onSuccess() {

				mTextInfo.append("connectDevice onSuccess\n");

				getConnectionInfo();
			}

			@Override
			public void onFailure(int reason) {
				mTextInfo.append("connectDevice failed\n");
			}
		});

	}

	private void getConnectionInfo() {

		mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
			@Override
			public void onConnectionInfoAvailable(WifiP2pInfo info) {
				if (info != null && info.groupOwnerAddress != null) {

					String ip = info.groupOwnerAddress.getHostAddress();
					mTextInfo.append("getConnectionInfo:\n groupFormed:" + info.groupFormed + "," + ip + " \n isGroupOwner:" + info.isGroupOwner + "\n");

					if (info.groupFormed) {
						isService = info.isGroupOwner;
						if (isService) {
							startService();
						} else {
							startSend(ip);
						}
						startService();
					}

				}
			}
		});
	}


	private ServerSocket mServerSocket;
	private Socket mResponseClient;

	private void startService() {
		new Thread() {
			@Override
			public void run() {
				try {
					//创建socket监听
					if (mServerSocket != null) {
						mServerSocket.close();
					}

					mServerSocket = new ServerSocket(8686);

					if (mServerSocket != null) {

						mResponseClient = mServerSocket.accept();

						printMessage("ServerSocket:" + 8686);

						BufferedReader in = new BufferedReader(new InputStreamReader(mResponseClient.getInputStream(), "UTF-8"));

						String str = in.readLine();
						String client_ip = mResponseClient.getInetAddress().getHostAddress();

						printMessage("From Client: ip_" + client_ip + ", message:" + str);

						// 回复给客户端
						Writer writer = new OutputStreamWriter(mResponseClient.getOutputStream());
						writer.write("result_success\n");
						writer.flush();
						writer.close();

						//in.close();

					}

					//mServerSocket.close();
					//socket.close();

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}.start();
	}


	private Socket clientSocket;

	private void startSend(final String host) {
		new Thread() {
			@Override
			public void run() {
				try {
					clientSocket = new Socket();
					int port = 8686;

					Thread.sleep(3000); // 等服务端起来

					//根据server端的地址和端口建立socket，并设置超时
					clientSocket.connect(new InetSocketAddress(host, port), 5000);

					/*Writer writer = new OutputStreamWriter(clientSocket.getOutputStream());
					writer.write("I am Client\n");
					writer.flush();
					//writer.close();*/

					sendMessage(clientSocket, "I am Client");

					printMessage("Send to Service");

					BufferedReader in = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));

					String str = in.readLine();
					String client_ip = clientSocket.getInetAddress().getHostAddress();
					//in.close();

					printMessage("From Service:" + str);

				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					/*if (clientSocket != null) {
						if (clientSocket.isConnected()) {
							try {
								clientSocket.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					}*/
				}
			}

		}.start();
	}

	private void sendMessage(Socket socket, String msg) {
		if (socket!= null && !socket.isClosed()) {
			try {
				Writer writer = new OutputStreamWriter(socket.getOutputStream());
				writer.write(msg + "\n");
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void printMessage(final String str) {
		mTextInfo.post(new Runnable() {
			@Override
			public void run() {
				mTextInfo.append(str + "\n");
			}
		});
	}

	

}
