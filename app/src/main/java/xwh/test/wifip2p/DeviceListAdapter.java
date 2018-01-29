package xwh.test.wifip2p;

import android.app.Activity;
import android.net.wifi.p2p.WifiP2pDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by xwh on 2018/1/29.
 */

public class DeviceListAdapter extends BaseAdapter {
	private List<WifiP2pDevice> mDevices;
	private Activity mContext;

	public DeviceListAdapter(Activity context) {
		mContext = context;
	}

	public void setData(List<WifiP2pDevice> devices) {
		mDevices = devices;
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return mDevices == null ? 0 : mDevices.size();
	}

	@Override
	public WifiP2pDevice getItem(int position) {
		return mDevices.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = mContext.getLayoutInflater().inflate(R.layout.layout_device, null);
		}

		TextView textView = convertView.findViewById(R.id.item_name);
		WifiP2pDevice wifiP2pDevice = mDevices.get(position);

		textView.setText(wifiP2pDevice.deviceName);

		return convertView;
	}
}
