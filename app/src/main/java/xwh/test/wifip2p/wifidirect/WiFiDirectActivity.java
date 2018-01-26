package xwh.test.wifip2p.wifidirect;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import xwh.test.wifip2p.R;


public class WiFiDirectActivity extends Activity implements ChannelListener,DeviceListFragment.DeviceActionListener {

    public static final String TAG = "wifidirect";
    private WifiP2pManager mManager;
    private Channel mChannel;
    
    private IntentFilter intentFilter = new IntentFilter();
    private BroadcastReceiver mReceiver;
    
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.setContentView(R.layout.main);
        //��ʼ��WifiP2pManager
        mManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        
        //������Ҫ������action
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        this.findViewById(R.id.bt_discover).setOnClickListener(itemOnClickListener);
        
    }

    //ע�����
    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new WifiDirectReceiver(mManager,mChannel,this);
        registerReceiver(mReceiver, intentFilter);
    }

    //ȡ������
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
    
    private View.OnClickListener itemOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View item) {
            switch (item.getId()) {
                case R.id.atn_direct_enable:
                    if (null != mManager && null != mChannel) {
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    } else {
                        Log.e(TAG, "channel or manager is null");
                    }
                    break;
                case R.id.bt_discover:
                    if (!isWifiP2pEnabled) {
                        Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager().findFragmentById(R.id.frag_list);
                    fragment.onInitiateDiscovery();

                    mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                        @Override
                        public void onSuccess() {
                            Toast.makeText(WiFiDirectActivity.this, "Discovery initiated", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Toast.makeText(WiFiDirectActivity.this, "Discovery Failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    };


    public void setIsWifiP2pEnabled(boolean isWifiEnabled){
        this.isWifiP2pEnabled = isWifiEnabled;
    }
    
    public void resetData(){
        DeviceListFragment fragList = (DeviceListFragment)getFragmentManager().findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragDetail = (DeviceDetailFragment)getFragmentManager().findFragmentById(R.id.frag_detail);
        if(null != fragList){
            fragList.clearPeers();
        }
        if(fragDetail != null){
            fragDetail.resetViews();
        }
       
    }

    @Override
    public void onChannelDisconnected() {
        if(mManager != null && !retryChannel){
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_SHORT).show();
            resetData();
            retryChannel = true;
            mManager.initialize(this, getMainLooper(), null);
        }else{
            Toast.makeText(this, "Server is probably lost premanently", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showDetail(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment)getFragmentManager().findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);
    }

    @Override
    public void cancelDisconnect() {
        if(mManager != null){
            final DeviceListFragment fragment = (DeviceListFragment)getFragmentManager().findFragmentById(R.id.frag_list);
            if(fragment.getDevice() == null || 
                    fragment.getDevice().status == WifiP2pDevice.CONNECTED){
                disconnect();
            }else if(fragment.getDevice().status == WifiP2pDevice.AVAILABLE || 
                    fragment.getDevice().status == WifiP2pDevice.INVITED){
                mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {
                    
                    @Override
                    public void onSuccess() {
                        
                    }
                    
                    @Override
                    public void onFailure(int reason) {
                        
                    }
                });
            }
        }
    }

    @Override
    public void connect(WifiP2pConfig config) {
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            
            @Override
            public void onSuccess() {
                
            }
            
            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment)getFragmentManager().findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            
            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }
            
            @Override
            public void onFailure(int reason) {
                Log.e(WiFiDirectActivity.TAG, "disconnect faile reason: "+reason);
            }
        });
    }
    
}