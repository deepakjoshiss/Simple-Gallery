package com.simplemobiletools.gallery.pro.aes;

import android.content.Context;
import android.graphics.PorterDuff;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.simplemobiletools.gallery.pro.R;

import java.util.ArrayList;
import java.util.Collections;

/* loaded from: classes.dex */
public class ClassicLockView extends LinearLayout implements View.OnClickListener, TextWatcher {
    public View mBtn0;
    public View mBtn1;
    public View mBtn2;
    public View mBtn3;
    public View mBtn4;
    public View mBtn5;
    public View mBtn6;
    public View mBtn7;
    public View mBtn8;
    public View mBtn9;
    public ArrayList<View> mBtns;
    public View mDividerInput;
    public EditText mEPass;
    public String mInput;
    public boolean mIsSetup;
    public ImageView mIvBackSpace;
    public String mOldInput;
    public int mPasswordLevel;
    public TextView mTv0;
    public TextView mTv1;
    public TextView mTv2;
    public TextView mTv3;
    public TextView mTv4;
    public TextView mTv5;
    public TextView mTv6;
    public TextView mTv7;
    public TextView mTv8;
    public TextView mTv9;
    public ArrayList<TextView> mTvChars;
    public ArrayList<TextView> mTvNumbers;
    public ImageView mGo;
    private TextView mLabel;

    private PassCallback mPassCallback;

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
    }

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    public final void initData() {
    }

    public ClassicLockView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mBtns = new ArrayList<>();
        this.mTvNumbers = new ArrayList<>();
        this.mTvChars = new ArrayList<>();
        this.mInput = "";
        this.mOldInput = "";
    }

    @Override // android.view.View
    public void onFinishInflate() {
        super.onFinishInflate();
        initData();
        this.mBtn0 = findViewById(R.id.button0);
        this.mBtn1 = findViewById(R.id.button1);
        this.mBtn2 = findViewById(R.id.button2);
        this.mBtn3 = findViewById(R.id.button3);
        this.mBtn4 = findViewById(R.id.button4);
        this.mBtn5 = findViewById(R.id.button5);
        this.mBtn6 = findViewById(R.id.button6);
        this.mBtn7 = findViewById(R.id.button7);
        this.mBtn8 = findViewById(R.id.button8);
        this.mBtn9 = findViewById(R.id.button9);
        this.mTv0 = (TextView) findViewById(R.id.tv0);
        this.mTv1 = (TextView) findViewById(R.id.tv1);
        this.mTv2 = (TextView) findViewById(R.id.tv2);
        this.mTv3 = (TextView) findViewById(R.id.tv3);
        this.mTv4 = (TextView) findViewById(R.id.tv4);
        this.mTv5 = (TextView) findViewById(R.id.tv5);
        this.mTv6 = (TextView) findViewById(R.id.tv6);
        this.mTv7 = (TextView) findViewById(R.id.tv7);
        this.mTv8 = (TextView) findViewById(R.id.tv8);
        this.mTv9 = (TextView) findViewById(R.id.tv9);
        this.mBtns.add(this.mBtn0);
        this.mBtns.add(this.mBtn1);
        this.mBtns.add(this.mBtn2);
        this.mBtns.add(this.mBtn3);
        this.mBtns.add(this.mBtn4);
        this.mBtns.add(this.mBtn5);
        this.mBtns.add(this.mBtn6);
        this.mBtns.add(this.mBtn7);
        this.mBtns.add(this.mBtn8);
        this.mBtns.add(this.mBtn9);
        this.mTvNumbers.add(this.mTv0);
        this.mTvNumbers.add(this.mTv1);
        this.mTvNumbers.add(this.mTv2);
        this.mTvNumbers.add(this.mTv3);
        this.mTvNumbers.add(this.mTv4);
        this.mTvNumbers.add(this.mTv5);
        this.mTvNumbers.add(this.mTv6);
        this.mTvNumbers.add(this.mTv7);
        this.mTvNumbers.add(this.mTv8);
        this.mTvNumbers.add(this.mTv9);
        this.mTvChars.add((TextView) findViewById(R.id.tvChar0));
        this.mTvChars.add((TextView) findViewById(R.id.tvChar1));
        this.mTvChars.add((TextView) findViewById(R.id.tvChar2));
        this.mTvChars.add((TextView) findViewById(R.id.tvChar3));
        this.mTvChars.add((TextView) findViewById(R.id.tvChar4));
        this.mTvChars.add((TextView) findViewById(R.id.tvChar5));
        this.mTvChars.add((TextView) findViewById(R.id.tvChar6));
        this.mTvChars.add((TextView) findViewById(R.id.tvChar7));
        this.mTvChars.add((TextView) findViewById(R.id.tvChar8));
        this.mTvChars.add((TextView) findViewById(R.id.tvChar9));

        this.mLabel = findViewById(R.id.tvLabel);
        this.mIvBackSpace = findViewById(R.id.ivBackSpace);
        this.mIvBackSpace.setOnClickListener(this);

        this.mGo = findViewById(R.id.buttonOK);
        this.mGo.setOnClickListener(this);

        this.mEPass = findViewById(R.id.ePass);
        mEPass.addTextChangedListener(this);

        for (int i = 0; i < this.mBtns.size(); i++) {
            this.mBtns.get(i).setOnClickListener(this);
        }
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.ivBackSpace) {
            if (this.mInput.length() > 0) {
                String substring = this.mInput.substring(0, mInput.length() - 1);
                this.mInput = substring;
                this.mEPass.setText(substring);
                this.mEPass.setSelection(this.mInput.length());
                return;
            }
            return;
        }
        switch (id) {
            case R.id.button0 /* 2131361970 */:
                String str2 = this.mInput + this.mTv0.getText().toString();
                this.mInput = str2;
                this.mEPass.setText(str2);
                this.mEPass.setSelection(this.mInput.length());
                return;
            case R.id.button1 /* 2131361971 */:
                String str3 = this.mInput + this.mTv1.getText().toString();
                this.mInput = str3;
                this.mEPass.setText(str3);
                this.mEPass.setSelection(this.mInput.length());
                return;
            case R.id.button2 /* 2131361972 */:
                String str4 = this.mInput + this.mTv2.getText().toString();
                this.mInput = str4;
                this.mEPass.setText(str4);
                this.mEPass.setSelection(this.mInput.length());
                return;
            case R.id.button3 /* 2131361973 */:
                String str5 = this.mInput + this.mTv3.getText().toString();
                this.mInput = str5;
                this.mEPass.setText(str5);
                this.mEPass.setSelection(this.mInput.length());
                return;
            case R.id.button4 /* 2131361974 */:
                String str6 = this.mInput + this.mTv4.getText().toString();
                this.mInput = str6;
                this.mEPass.setText(str6);
                this.mEPass.setSelection(this.mInput.length());
                return;
            case R.id.button5 /* 2131361975 */:
                String str7 = this.mInput + this.mTv5.getText().toString();
                this.mInput = str7;
                this.mEPass.setText(str7);
                this.mEPass.setSelection(this.mInput.length());
                return;
            case R.id.button6 /* 2131361976 */:
                String str8 = this.mInput + this.mTv6.getText().toString();
                this.mInput = str8;
                this.mEPass.setText(str8);
                this.mEPass.setSelection(this.mInput.length());
                return;
            case R.id.button7 /* 2131361977 */:
                String str9 = this.mInput + this.mTv7.getText().toString();
                this.mInput = str9;
                this.mEPass.setText(str9);
                this.mEPass.setSelection(this.mInput.length());
                return;
            case R.id.button8 /* 2131361978 */:
                String str10 = this.mInput + this.mTv8.getText().toString();
                this.mInput = str10;
                this.mEPass.setText(str10);
                this.mEPass.setSelection(this.mInput.length());
                return;
            case R.id.button9 /* 2131361979 */:
                String str11 = this.mInput + this.mTv9.getText().toString();
                this.mInput = str11;
                this.mEPass.setText(str11);
                this.mEPass.setSelection(this.mInput.length());
                return;
            case R.id.buttonOK:
                this.mPassCallback.onGoClick(this.mEPass.getText().toString());
                return;
            default:
                return;
        }
    }

    @Override // android.text.TextWatcher
    public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        this.mPassCallback.onTextChange(this.mEPass.getText().toString());
    }

    public final void resetInput() {
        this.mEPass.setText("");
        this.mInput = "";
    }

    public void setPassCallback(PassCallback callback) {
        this.mPassCallback = callback;
    }

    public void setLabel(String label) {
        mLabel.setText(label);
    }

    public void setTextColor(int i) {
        for (int i2 = 0; i2 < this.mBtns.size(); i2++) {
            this.mTvNumbers.get(i2).setTextColor(i);
            this.mTvChars.get(i2).setTextColor(i);
        }
        this.mIvBackSpace.setColorFilter(i, PorterDuff.Mode.SRC_IN);
        this.mEPass.setTextColor(i);
        this.mDividerInput.setBackgroundColor(i);
    }
}
