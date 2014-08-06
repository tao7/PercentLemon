/*
 * Copyright (C) 2014 Chang Wentao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.changwentao.lemon;

import java.util.Random;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import cn.changwentao.widget.PercentLemon;

public class MainActivity extends Activity implements View.OnClickListener {
	private final Random random = new Random();
	private PercentLemon lemonLeft;
	private PercentLemon lemonRight;
	private Button buttonLeft;
	private Button buttonRight;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		lemonLeft = (PercentLemon) findViewById(R.id.lemonLeft);
		lemonRight = (PercentLemon) findViewById(R.id.lemonRight);
		buttonLeft = (Button) findViewById(R.id.buttonLeft);
		buttonRight = (Button) findViewById(R.id.buttonRight);

		buttonLeft.setOnClickListener(this);
		buttonRight.setOnClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_about) {
			AlertDialog.Builder builder = new Builder(MainActivity.this);
			builder.setTitle("关于");
			builder.setMessage("百分比圆环Sample");
			builder.setPositiveButton("确定", null).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		float f = random.nextFloat();
		switch (v.getId()) {
		case R.id.buttonLeft:
			lemonLeft.animatToPercent(f * 100);
			break;
		case R.id.buttonRight:
			lemonRight.animatToPercent(f * 100);
			break;
		}
	}

}
