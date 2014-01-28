package org.tigase.messenger.phone.pro;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.about, container, false);

		TextView tos = (TextView) view.findViewById(R.id.aboutTermsOfService);
		tos.setText(Html.fromHtml("<a href='" + getResources().getString(R.string.about_terms_of_service_url) + "'>"
				+ getResources().getString(R.string.about_terms_of_service) + "</a>"));
		tos.setMovementMethod(LinkMovementMethod.getInstance());
		TextView pp = (TextView) view.findViewById(R.id.aboutPrivacyPolicy);
		pp.setText(Html.fromHtml("<a href='" + getResources().getString(R.string.about_privacy_policy_url) + "'>"
				+ getResources().getString(R.string.about_privacy_policy) + "</a>"));
		pp.setMovementMethod(LinkMovementMethod.getInstance());

		return view;
	}

}
