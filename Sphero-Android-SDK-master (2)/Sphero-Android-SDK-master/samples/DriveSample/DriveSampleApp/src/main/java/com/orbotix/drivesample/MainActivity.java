package com.orbotix.drivesample;

import android.media.audiofx.Visualizer;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.orbotix.ConvenienceRobot;
import com.orbotix.DualStackDiscoveryAgent;
import com.orbotix.Ollie;
import com.orbotix.Sphero;
import com.orbotix.calibration.api.CalibrationEventListener;
import com.orbotix.calibration.api.CalibrationImageButtonView;
import com.orbotix.calibration.api.CalibrationView;
import com.orbotix.classic.DiscoveryAgentClassic;
import com.orbotix.classic.RobotClassic;
import com.orbotix.colorpicker.api.ColorPickerEventListener;
import com.orbotix.colorpicker.api.ColorPickerFragment;
import com.orbotix.common.*;
import com.orbotix.joystick.api.JoystickEventListener;
import com.orbotix.joystick.api.JoystickView;
import com.orbotix.le.DiscoveryAgentLE;
import com.orbotix.le.RobotLE;
import com.orbotix.robotpicker.RobotPickerDialog;
//import android.support.design.widget.Snackbar;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity implements RobotPickerDialog.RobotPickerListener,
                                                      DiscoveryAgentEventListener,
                                                      RobotChangedStateListener {

    static final String RECORDED_FILE="/sdcard/reco";
    MediaPlayer player;
    MediaRecorder recorder;
    private Timer _timer;
    int [] temp= new int[100] ;
    int counter=0;
    int margin=30000;
    boolean flag_3000=false;
    private static final String TAG = "MainActivity";



    /**
     * Our current discovery agent that we will use to find robots of a certain protocol
     */
    private DiscoveryAgent _currentDiscoveryAgent;

    /**
     * The dialog that will allow the user to chose which type of robot to connect to
     */
    private RobotPickerDialog _robotPickerDialog;

    /**
     * The joystick that we will use to send roll commands to the robot
     */
    private JoystickView _joystick;

    /**
     * The connected robot
     */
    private ConvenienceRobot _connectedRobot;

    /**
     * The calibration view, used for setting the default heading of the robot
     */
    private CalibrationView _calibrationView;

    /**
     * A button used for one finger calibration
     */
    private CalibrationImageButtonView _calibrationButtonView;

    /**
     * The fragment to show that contains the color picker
     */
    private ColorPickerFragment _colorPicker;

    /**
     * The button used to bring up the color picker
     */
    private Button _colorPickerButton;

    /**
     * The button to set developer mode
     */
    private Switch _developerModeSwitch;
    private float angle = 0;
    private long distance=0;   //time
    private float power=0;  //speed
    private float red=0, green=0, blue=0; //color
    Button go_btn;
    /**
     * Reference to the layout containing the developer mode switch and label
     */
    private LinearLayout _developerModeLayout;
    private  static  final int RESULT_SPEECH=1;
    private Intent i;
    private TextView tv;
    private Button bt3;
    private Button bt2;
    private Button user_bt;
    private android.speech.SpeechRecognizer mRecognizer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        final RadioGroup Color1 = (RadioGroup) findViewById(R.id.radioGroup3);
        final RadioGroup Color2 = (RadioGroup) findViewById(R.id.radioGroup4);
        final RadioGroup Angle1 = (RadioGroup) findViewById(R.id.radioGroup1);
        final RadioGroup Angle2 = (RadioGroup) findViewById(R.id.radioGroup2);
        final RadioGroup checkBoxW = (RadioGroup) findViewById(R.id.radioGroup5);
        final RadioGroup checkBoxM = (RadioGroup) findViewById(R.id.radioGroup6);
        final RadioGroup checkBoxS = (RadioGroup) findViewById(R.id.radioGroup7);

        setupJoystick();
        setupCalibration();
        setupColorPicker();
        _timer = new Timer();

        i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

        // Here, you need to route all the touch events to the joystick and calibration view so that they know about
        // them. To do this, you need a way to reference the view (in this case, the id "entire_view") and attach
        // an onTouchListener which in this case is declared anonymously and invokes the
        // Controller#interpretMotionEvent() method on the joystick and the calibration view.

        Button start_v = (Button) findViewById(R.id.startv);
        start_v.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {

                /* 사용자의 OS 버전이 마시멜로우 이상인지 체크한다. */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    /* 사용자 단말기의 권한 중 "전화걸기" 권한이 허용되어 있는지 체크한다.
                    *  int를 쓴 이유? 안드로이드는 C기반이기 때문에, Boolean 이 잘 안쓰인다.
                    */
                    int permissionResult = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
                    int permissionResult2 = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
                    int permissionResult3 = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    /* CALL_PHONE의 권한이 없을 때 */
                    // 패키지는 안드로이드 어플리케이션의 아이디다.( 어플리케이션 구분자 )
                    if ((permissionResult == PackageManager.PERMISSION_DENIED) && (permissionResult2 == PackageManager.PERMISSION_DENIED) && (permissionResult3 == PackageManager.PERMISSION_DENIED)) {


                        /* 사용자가 CALL_PHONE 권한을 한번이라도 거부한 적이 있는 지 조사한다.
                        * 거부한 이력이 한번이라도 있다면, true를 리턴한다.
                        */
                        if ((shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) && (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) && (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {

                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle("권한이 필요합니다.")
                                    .setMessage("이 기능을 사용하기 위해서는 단말기의 오디오 권한이 필요합니다. 계속하시겠습니까?")
                                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
                                            }

                                        }
                                    })
                                    .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(MainActivity.this, "기능을 취소했습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .create()
                                    .show();
                        }

                        //최초로 권한을 요청할 때
                        else {
                            // CALL_PHONE 권한을 Android OS 에 요청한다.
                            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
                        }

                    }
                    /* CALL_PHONE의 권한이 있을 때 */
                    else {
                        Toast.makeText(MainActivity.this, "success", Toast.LENGTH_LONG).show();

                    }

                }
                /* 사용자의 OS 버전이 마시멜로우 이하일 떄 */
                else {

                }
                //------
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    /* 사용자 단말기의 권한 중 "전화걸기" 권한이 허용되어 있는지 체크한다.
                    *  int를 쓴 이유? 안드로이드는 C기반이기 때문에, Boolean 이 잘 안쓰인다.
                    */

                    int permissionResult2 = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
                    int permissionResult3 = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    /* CALL_PHONE의 권한이 없을 때 */
                    // 패키지는 안드로이드 어플리케이션의 아이디다.( 어플리케이션 구분자 )
                    if ((permissionResult2 == PackageManager.PERMISSION_DENIED)) {


                        /* 사용자가 CALL_PHONE 권한을 한번이라도 거부한 적이 있는 지 조사한다.
                        * 거부한 이력이 한번이라도 있다면, true를 리턴한다.
                        */
                        if ((shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))) {

                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle("권한이 필요합니다.")
                                    .setMessage("이 기능을 사용하기 위해서는 단말기의 오디오 권한이 필요합니다. 계속하시겠습니까?")
                                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
                                            }

                                        }
                                    })
                                    .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(MainActivity.this, "기능을 취소했습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .create()
                                    .show();
                        }

                        //최초로 권한을 요청할 때
                        else {
                            // CALL_PHONE 권한을 Android OS 에 요청한다.
                            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
                        }

                    }
                    /* CALL_PHONE의 권한이 있을 때 */
                    else {
                        Toast.makeText(MainActivity.this, "success", Toast.LENGTH_LONG).show();
                        if (recorder != null) {
                            recorder.stop();
                            recorder.release();
                            recorder = null;
                        }// TODO Auto-generated method stub

                        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
                        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                        recorder.setAudioSamplingRate(48000);
                        recorder.setAudioEncodingBitRate(96000);
                        recorder.setAudioChannels(2);
                        recorder.setOutputFile(RECORDED_FILE);
                        try {
                            Toast.makeText(getApplicationContext(),
                                    "녹음을 시작합니다.", Toast.LENGTH_SHORT).show();
                            recorder.prepare();
                            recorder.start();


                        } catch (Exception ex) {

                        }


                    }

                }
                /* 사용자의 OS 버전이 마시멜로우 이하일 떄 */
                else {

                    Toast.makeText(MainActivity.this, "success", Toast.LENGTH_LONG).show();
                }
//-------------------------
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    /* 사용자 단말기의 권한 중 "전화걸기" 권한이 허용되어 있는지 체크한다.
                    *  int를 쓴 이유? 안드로이드는 C기반이기 때문에, Boolean 이 잘 안쓰인다.
                    */

                    int permissionResult2 = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
                    int permissionResult3 = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

                    /* CALL_PHONE의 권한이 없을 때 */
                    // 패키지는 안드로이드 어플리케이션의 아이디다.( 어플리케이션 구분자 )
                    if ((permissionResult3 == PackageManager.PERMISSION_DENIED)) {


                        /* 사용자가 CALL_PHONE 권한을 한번이라도 거부한 적이 있는 지 조사한다.
                        * 거부한 이력이 한번이라도 있다면, true를 리턴한다.
                        */
                        if ((shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {

                            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                            dialog.setTitle("권한이 필요합니다.")
                                    .setMessage("이 기능을 사용하기 위해서는 단말기의 오디오 권한이 필요합니다. 계속하시겠습니까?")
                                    .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                                            }

                                        }
                                    })
                                    .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Toast.makeText(MainActivity.this, "기능을 취소했습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .create()
                                    .show();
                        }

                        //최초로 권한을 요청할 때
                        else {
                            // CALL_PHONE 권한을 Android OS 에 요청한다.
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
                        }

                    }
                    /* CALL_PHONE의 권한이 있을 때 */
                    else {
                        Toast.makeText(MainActivity.this, "success", Toast.LENGTH_LONG).show();

                    }

                }
                /* 사용자의 OS 버전이 마시멜로우 이하일 떄 */
                else {

                    Toast.makeText(MainActivity.this, "success", Toast.LENGTH_LONG).show();

                }

            }
        });

        Button start = (Button) findViewById(R.id.startvoice);
        start.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
                recorder.setOutputFile(RECORDED_FILE);
                try {
                    Toast.makeText(getApplicationContext(),
                            "녹음을 시작합니다.", Toast.LENGTH_LONG).show();
                    recorder.prepare();
                    recorder.start();
                    _timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((TextView) findViewById(R.id.freq)).setText(String.valueOf(recorder.getMaxAmplitude()));
                                    ((TextView) findViewById(R.id.flag_no)).setText(String.valueOf(flag_3000));

                                    temp[counter] = recorder.getMaxAmplitude();
                                    counter++;
                                }
                            });
                        }
                    }, 1000, 1000);

                } catch (Exception ex) {

                }
            }
        });

        Button stop = (Button) findViewById(R.id.stopvoice);
        stop.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recorder == null)
                    return;

                recorder.stop();
                recorder.release();
                _timer.cancel();
                recorder = null;

                Toast.makeText(getApplicationContext(),
                        "녹음이 중지되었습니다.", Toast.LENGTH_LONG).show();

                // TODO : click event
            }
        });

        Button play_v = (Button) findViewById(R.id.playrecord);
        play_v.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (player != null) {
                        try {
                            player.release();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }


                    player = new MediaPlayer();
                    player.setDataSource(RECORDED_FILE);
                    player.prepare();
                    player.start();


                    Toast.makeText(getApplicationContext(), "음악파일 재생 시작됨.", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


            // TODO : click event

        });

        Button stop_play = (Button) findViewById(R.id.stoprecord);
        stop_play.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                player.pause();
                Toast.makeText(getApplicationContext(), "음악 파일 재생 중지됨.", Toast.LENGTH_SHORT).show();

                int i;
                for (i = 0; i < counter; i++) {
                    if (temp[i] >= 30000) {
                        flag_3000 = true;
                        break;
                    } else
                        ;

                }
                ((TextView) findViewById(R.id.flag_no)).setText(String.valueOf(flag_3000));

                // TODO : click event
            }
        });

        final Button fab = (Button) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Toast toast = Toast.makeText(getApplicationContext(), "Replace with your own action", Toast.LENGTH_LONG);
                //snackbar.make(view, " Replace with your own action", Snackbar.LENGTH_LONG).show();
                toast.show();
                toast.cancel();
            }
        });

        tv = (TextView) findViewById(R.id.tv);
        bt3 = (Button) findViewById(R.id.bt);
        user_bt = (Button) findViewById(R.id.user_bt);

        bt3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (view.getId() == R.id.bt) {
                    i.putExtra(RecognizerIntent.EXTRA_PROMPT, "start-speak");
                    Toast.makeText(MainActivity.this, "start-speak", Toast.LENGTH_SHORT).show();
                    try {
                        startActivityForResult(i, RESULT_SPEECH);
                        //   Toast.makeText(MainActivity.this,"음성인식완료",Toast.LENGTH_SHORT).show();
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getApplicationContext(), "Speech to text 지원 ㄴㄴ", Toast.LENGTH_SHORT).show();
                        e.getStackTrace();
                    }
                }
            }
        });

        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mRecognizer.setRecognitionListener(listener);

        bt2 = (Button) findViewById(R.id.bt2);
        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecognizer.startListening(i);
            }
        });

        //COLOR
        Color1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.red) {
                    red = 255;
                    green = 0;
                    blue = 0;
                    Color2.clearCheck();
                } else if (checkedId == R.id.orange) {
                    red = 204;
                    green = 0;
                    blue = 0;
                    Color2.clearCheck();
                } else if (checkedId == R.id.yellow) {
                    red = 255;
                    green = 204;
                    blue = 0;
                    Color2.clearCheck();
                } else if (checkedId == R.id.green) {
                    red = 0;
                    green = 255;
                    blue = 0;
                    Color2.clearCheck();
                }
            }
        });

        Color2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.blue) {
                    red = 0;
                    green = 0;
                    blue = 255;
                    Color1.clearCheck();
                } else if (checkedId == R.id.plum) {
                    red = 255;
                    green = 0;
                    blue = 0;
                    Color1.clearCheck();
                } else if (checkedId == R.id.purple) {
                    red = 204;
                    green = 0;
                    blue = 153;
                    Color1.clearCheck();
                } else if (checkedId == R.id.black) {
                    red = 0;
                    green = 0;
                    blue = 0;
                    Color1.clearCheck();
                }
            }
        });


        findViewById(R.id.entire_view).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                _joystick.interpretMotionEvent(event);
                _calibrationView.interpretMotionEvent(event);
                return true;
            }
        });

        //ANGLE
        Angle1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.deg1) {
                    angle = 45;
                    Angle2.clearCheck();
                } else if (checkedId == R.id.deg2) {
                    angle = 90;
                    Angle2.clearCheck();
                } else if (checkedId == R.id.deg3) {
                    angle = 135;
                    Angle2.clearCheck();
                } else if (checkedId == R.id.deg4) {
                    angle = 180;
                    Angle2.clearCheck();
                }
            }
        });

        Angle2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.deg5) {
                    angle = 225;
                    Angle1.clearCheck();
                } else if (checkedId == R.id.deg6) {
                    angle = 270;
                    Angle1.clearCheck();
                } else if (checkedId == R.id.deg7) {
                    angle = 315;
                    Angle1.clearCheck();
                }
                else if (checkedId == R.id.deg8) {
                    angle = 360;
                    Angle1.clearCheck();
                }
            }
        });

        //DISTANCE & POWER
        checkBoxW.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.short_w) {
                    power = (float) 0.7;
                    distance = 700;
                    checkBoxM.clearCheck();
                    checkBoxS.clearCheck();
                } else if (checkedId == R.id.short_m) {
                    power = (float) 1.5;
                    distance = 700;
                    checkBoxM.clearCheck();
                    checkBoxS.clearCheck();
                } else if (checkedId == R.id.short_s) {
                    power = (float) 2.2;
                    distance = 700;
                    checkBoxM.clearCheck();
                    checkBoxS.clearCheck();
                }
            }
        });

        checkBoxM.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.medium_w) {
                    power = (float) 0.7;
                    distance = 1100;
                    checkBoxW.clearCheck();
                    checkBoxS.clearCheck();
                } else if (checkedId == R.id.medium_m) {
                    power = (float) 1.5;
                    distance = 1100;
                    checkBoxW.clearCheck();
                    checkBoxS.clearCheck();
                } else if (checkedId == R.id.medium_s) {
                    power = (float) 2.2;
                    distance = 1100;
                    checkBoxW.clearCheck();
                    checkBoxS.clearCheck();
                }
            }
        });

        checkBoxS.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.long_w) {
                    power = (float) 0.7;
                    distance = 1500;
                    checkBoxW.clearCheck();
                    checkBoxM.clearCheck();
                } else if (checkedId == R.id.long_m) {
                    power = (float) 1.5;
                    distance = 1500;
                    checkBoxW.clearCheck();
                    checkBoxM.clearCheck();
                } else if (checkedId == R.id.long_s) {
                    power = (float) 2.2;
                    distance = 1500;
                    checkBoxW.clearCheck();
                    checkBoxM.clearCheck();
                }
            }
        });

        go_btn = (Button) findViewById(R.id.go_btn);
        go_btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                    _connectedRobot.setLed(red, green, blue);

                    _connectedRobot.drive(angle, power);
                    SystemClock.sleep(distance);
                    _connectedRobot.stop();

                    //initialize
                    Color1.clearCheck();
                    Color2.clearCheck();
                    Angle1.clearCheck();
                    Angle2.clearCheck();
                    checkBoxW.clearCheck();
                    checkBoxM.clearCheck();
                    checkBoxS.clearCheck();
            }
        });
    }
private RecognitionListener listener = new RecognitionListener() {
    @Override
    public void onReadyForSpeech(Bundle params){
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {

    }

    @Override
    public void onResults(Bundle results) {
        String key = "";
        key = SpeechRecognizer.RESULTS_RECOGNITION;
        ArrayList<String> mResult = results.getStringArrayList(key);
        String[] rs = new String[mResult.size()];
        mResult.toArray(rs);
        tv.setText(""+rs[0]);

      //  record=rs[0];
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
};
   protected void onActivityResult(int requestCode, int resultCode,Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK&&(requestCode==RESULT_SPEECH)){
            ArrayList<String> sstResult = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String result_sst = sstResult.get(0);
            tv.setText(""+result_sst);
           // Toast.makeText(MainActivity.this,result_sst,Toast.LENGTH_SHORT).show();

            //COLOR
            if(result_sst.equals("빨간색") || result_sst.equals("빨강색")== true) {
                _connectedRobot.setLed(255,0,0);
            }
            else if(result_sst.equals("주황색") || result_sst.equals("오렌지색")== true) {
                _connectedRobot.setLed(204,10,0);
            }
            else if(result_sst.equals("노랑색") || result_sst.equals("노란색")== true) {
                _connectedRobot.setLed(255,204,0);
            }
            else if(result_sst.equals("초록색") || result_sst.equals("그린")== true) {
                _connectedRobot.setLed(0,255,0);
            }
            else if(result_sst.equals("파랑색") || result_sst.equals("파란색")== true) {
                _connectedRobot.setLed(0,0,255);
            }
            else if(result_sst.equals("자두색") || result_sst.equals("플럼")== true) {
                _connectedRobot.setLed(204,0,50);
            }
            else if(result_sst.equals("보라색") || result_sst.equals("퍼플")== true) {
                _connectedRobot.setLed(204,0,153);
            }
            else if(result_sst.equals("검정색") || result_sst.equals("껌정색")== true) {
                _connectedRobot.setLed(0,0,0);
            }

            if(result_sst.equals("앞으로") || result_sst.equals("위로")== true){
                _connectedRobot.drive(0,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("뒤로") || result_sst.equals("아래로") == true){
                _connectedRobot.drive(180,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("오른쪽으로")||result_sst.equals("오른쪽")== true){
                _connectedRobot.drive(90,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("왼쪽으로")||result_sst.equals("왼쪽")== true){
                _connectedRobot.drive(270,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("왼쪽위로")|| result_sst.equals("왼쪽 위로")== true){
                _connectedRobot.drive(315,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("왼쪽아래로")||result_sst.equals("왼쪽 아래로")== true){
                _connectedRobot.drive(225,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("오른쪽위로")||result_sst.equals("오른쪽 위로")== true){
                _connectedRobot.drive(45,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("오른쪽아래로")||result_sst.equals("오른쪽 아래로")== true){
                _connectedRobot.drive(135,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("1시방향")||result_sst.equals("1시 방향")== true){
                _connectedRobot.drive((float)22.5,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("2시방향")||result_sst.equals("2시 방향")== true){
                _connectedRobot.drive((float)67.5,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("4시방향")||result_sst.equals("4시 방향")== true){
                _connectedRobot.drive((float)115.5,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("5시방향")||result_sst.equals("5시 방향")== true){
                _connectedRobot.drive((float)157.5,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("7시방향")||result_sst.equals("7시 방향")== true){
                _connectedRobot.drive((float)202.5,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("8시방향")||result_sst.equals("8시 방향")== true){
                _connectedRobot.drive((float)247.5,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("10시방향")||result_sst.equals("10시 방향")== true){
                _connectedRobot.drive((float)292.5,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }
            else if(result_sst.equals("11시방향")||result_sst.equals("11시 방향")== true){
                _connectedRobot.drive((float)337.5,(float)3);
                SystemClock.sleep(1500);
                _connectedRobot.stop();
            }

        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Create a robot picker dialog, this allows the user to select which robot they would like to connect to.
        // We don't need to do this step if we know which robot we want to talk to, and don't need the user to
        // decide that.

        if (_robotPickerDialog == null) {
            _robotPickerDialog = new RobotPickerDialog(this, this);
        }
        // Show the picker only if it's not showing. This keeps multiple calls to onStart from showing too many pickers.
        if (!_robotPickerDialog.isShowing()) {
            _robotPickerDialog.show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (_currentDiscoveryAgent != null) {
            // When pausing, you want to make sure that you let go of the connection to the robot so that it may be
            // accessed from within other applications. Before you do that, it is a good idea to unregister for the robot
            // state change events so that you don't get the disconnection event while the application is closed.
            // This is accomplished by using DiscoveryAgent#removeRobotStateListener().
            _currentDiscoveryAgent.removeRobotStateListener(this);

            // Here we are only handling disconnecting robots if the user selected a type of robot to connect to. If you
            // didn't use the robot picker, you will need to check the appropriate discovery agent manually by using
            // DiscoveryAgent.getInstance().getConnectedRobots()
            for (Robot r : _currentDiscoveryAgent.getConnectedRobots()) {
                // There are a couple ways to disconnect a robot: sleep and disconnect. Sleep will disconnect the robot
                // in addition to putting it into standby mode. If you choose to just disconnect the robot, it will
                // use more power than if it were in standby mode. In the case of Ollie, the main LED light will also
                // turn a bright purple, indicating that it is on but disconnected. Unless you have a specific reason
                // to leave a robot on but disconnected, you should use Robot#sleep()
                r.sleep();
            }
        }
    }

    /**
     * Invoked when the user makes a selection on which robot they would like to use.
     * @param robotPicked The type of the robot that was selected
     */
    @Override
    public void onRobotPicked(RobotPickerDialog.RobotPicked robotPicked) {
        // Dismiss the robot picker so that the user doesn't keep clicking it and trying to start
        // discovery multiple times
        _robotPickerDialog.dismiss();
        switch (robotPicked) {
            // If the user picked a Sphero, you want to start the Bluetooth Classic discovery agent, as that is the
            // protocol that Sphero talks over. This will allow us to find a Sphero and connect to it.
            case Sphero:
                // To get to the classic discovery agent, you use DiscoveryAgentClassic.getInstance()
                _currentDiscoveryAgent = DiscoveryAgentClassic.getInstance();
                break;
            // If the user picked an Ollie, you want to start the Bluetooth LE discovery agent, as that is the protocol
            // that Ollie talks over. This will allow you to find an Ollie and connect to it.
            case Ollie:
                // To get to the LE discovery agent, you use DiscoveryAgentLE.getInstance()
                _currentDiscoveryAgent = DiscoveryAgentLE.getInstance();
                break;
        }

        // Now that we have a discovery agent, we will start discovery on it using the method defined below
        startDiscovery();
    }

    /**
     * Invoked when the discovery agent finds a new available robot, or updates and already available robot
     * @param robots The list of all robots, connected or not, known to the discovery agent currently
     */
    @Override
    public void handleRobotsAvailable(List<Robot> robots) {
        // Here we need to know which version of the discovery agent we are using, if we are to use Sphero, we need to
        // treat Spheros a little bit differently.
        if (_currentDiscoveryAgent instanceof DiscoveryAgentClassic) {
            // If we are using the classic discovery agent, and therefore using Sphero, we'll just connect to the first
            // one available that we get. Note that "available" in classic means paired to the phone and turned on.
            _currentDiscoveryAgent.connect(robots.get(0));
        }
        else if (_currentDiscoveryAgent instanceof DiscoveryAgentLE) {
            // If we are using the LE discovery agent, and therefore using Ollie, there's not much we need to do here.
            // The SDK will automatically connect to the robot that you touch the phone to, and you will get a message
            // saying that the robot has connected.
            // Note that this method is called very frequently and will cause your app to slow down if you log.
        }
    }

    /**
     * Invoked when a robot changes state. For example, when a robot connects or disconnects.
     * @param robot The robot whose state changed
     * @param type Describes what changed in the state
     */
    @Override
    public void handleRobotChangedState(Robot robot, RobotChangedStateNotificationType type) {
        // For the purpose of this sample, we'll only handle the connected and disconnected notifications
        switch (type) {
            // A robot was connected, and is ready for you to send commands to it.
            case Online:
                // When a robot is connected, this is a good time to stop discovery. Discovery takes a lot of system
                // resources, and if left running, will cause your app to eat the user's battery up, and may cause
                // your application to run slowly. To do this, use DiscoveryAgent#stopDiscovery().
                _currentDiscoveryAgent.stopDiscovery();
                // It is also proper form to not allow yourself to re-register for the discovery listeners, so let's
                // unregister for the available notifications here using DiscoveryAgent#removeDiscoveryListener().
                _currentDiscoveryAgent.removeDiscoveryListener(this);
                // Don't forget to turn on UI elements
                _joystick.setEnabled(true);
                _calibrationView.setEnabled(true);
                _colorPickerButton.setEnabled(true);
                _calibrationButtonView.setEnabled(true);

                // Depending on what was connected, you might want to create a wrapper that allows you to do some
                // common functionality related to the individual robots. You can always of course use the
                // Robot#sendCommand() method, but Ollie#drive() reads a bit better.
                if (robot instanceof RobotLE) {
                    _connectedRobot = new Ollie(robot);

                    // Ollie has a developer mode that will allow a developer to poke at Bluetooth LE data manually
                    // without being disconnected. Here we set up the button to be able to enable or disable
                    // developer mode on the robot.
                    setupDeveloperModeButton();
                }
                else if (robot instanceof RobotClassic) {
                    _connectedRobot = new Sphero(robot);
                }

                // Finally for visual feedback let's turn the robot green saying that it's been connected
                _connectedRobot.setLed(0f, 1f, 0f);

                break;
            case Disconnected:
                // When a robot disconnects, it is a good idea to disable UI elements that send commands so that you
                // do not have to handle the user continuing to use them while the robot is not connected
                _joystick.setEnabled(false);
                _calibrationView.setEnabled(false);
                _colorPickerButton.setEnabled(false);
                _calibrationButtonView.setEnabled(false);

                // Disable the developer mode button when the robot disconnects so that it can be set up if a LE robot
                // connectes again
                if (robot instanceof RobotLE && _developerModeLayout != null) {
                    _developerModeLayout.setVisibility(View.INVISIBLE);
                }

                // When a robot disconnects, you might want to start discovery so that you can reconnect to a robot.
                // Starting discovery on disconnect however can cause unintended side effects like connecting to
                // a robot with the application closed. You should think carefully of when to start and stop discovery.
                // In this case, we will not start discovery when the robot disconnects. You can uncomment the following line of
                // code to see the start discovery on disconnection in action.
//                startDiscovery();
                break;
            default:
                Log.v(TAG, "Not handling state change notification: " + type);
                break;
        }
    }

    /**
     * Sets up the joystick from scratch
     */
    private void setupJoystick() {
        // Get a reference to the joystick view so that we can use it to send roll commands
        _joystick = (JoystickView)findViewById(R.id.joystickView);
        // In order to get the events from the joystick, you need to implement the JoystickEventListener interface
        // (or declare it anonymously) and set the listener.
        _joystick.setJoystickEventListener(new JoystickEventListener() {
            /**
             * Invoked when the user starts touching on the joystick
             */
            @Override
            public void onJoystickBegan() {
                // Here you can do something when the user starts using the joystick.
            }

            /**
             * Invoked when the user moves their finger on the joystick
             * @param distanceFromCenter The distance from the center of the joystick that the user is touching from 0.0 to 1.0
             *                           where 0.0 is the exact center, and 1.0 is the very edge of the outer ring.
             * @param angle The angle from the top of the joystick that the user is touching.
             */
            @Override
            public void onJoystickMoved(double distanceFromCenter, double angle) {
                // Here you can use the joystick input to drive the connected robot. You can easily do this with the
                // ConvenienceRobot#drive() method
                // Note that the arguments do flip here from the order of parameters
                _connectedRobot.drive((float)angle, (float)distanceFromCenter);
            }

            /**
             * Invoked when the user stops touching the joystick
             */
            @Override
            public void onJoystickEnded() {
                // Here you can do something when the user stops touching the joystick. For example, we'll make it stop driving.
                _connectedRobot.stop();
            }
        });

        // It is also a good idea to disable the joystick when a robot is not connected so that you do not have to
        // handle the user using the joystick while there is no robot connected.
        _joystick.setEnabled(false);
    }

    /**
     * Sets up the calibration gesture and button
     */
    private void setupCalibration() {
        // Get the view from the xml file
        _calibrationView = (CalibrationView)findViewById(R.id.calibrationView);
        // Set the glow. You might want to not turn this on if you're using any intense graphical elements.
        _calibrationView.setShowGlow(true);
        // Register anonymously for the calibration events here. You could also have this class implement the interface
        // manually if you plan to do more with the callbacks.
        _calibrationView.setCalibrationEventListener(new CalibrationEventListener() {
            /**
             * Invoked when the user begins the calibration process.
             */
            @Override
            public void onCalibrationBegan() {
                // The easy way to set up the robot for calibration is to use ConvenienceRobot#calibrating(true)
                Log.v(TAG, "Calibration began!");
                _connectedRobot.calibrating(true);
            }

            /**
             * Invoked when the user moves the calibration ring
             * @param angle The angle that the robot has rotated to.
             */
            @Override
            public void onCalibrationChanged(float angle) {
                // The usual thing to do when calibration happens is to send a roll command with this new angle, a speed of 0
                // and the calibrate flag set.
                _connectedRobot.rotate(angle);
            }

            /**
             * Invoked when the user stops the calibration process
             */
            @Override
            public void onCalibrationEnded() {
                // This is where the calibration process is "committed". Here you want to tell the robot to stop as well as
                // stop the calibration process.
                _connectedRobot.stop();
                _connectedRobot.calibrating(false);
            }
        });
        // Like the joystick, turn this off until a robot connects.
        _calibrationView.setEnabled(false);

        // To set up the button, you need a calibration view. You get the button view, and then set it to the
        // calibration view that we just configured.
        _calibrationButtonView = (CalibrationImageButtonView) findViewById(R.id.calibrateButton);
        _calibrationButtonView.setCalibrationView(_calibrationView);
        _calibrationButtonView.setEnabled(false);
    }

    /**
     * Sets up a new color picker fragment from scratch
     */
    private void setupColorPicker() {
        // To start, make a color picker fragment
        _colorPicker = new ColorPickerFragment();
        // Make sure you register for the change events. You will want to send the result of the picker to the robot.
        _colorPicker.setColorPickerEventListener(new ColorPickerEventListener() {

            /**
             * Called when the user changes the color picker
             * @param red The selected red component
             * @param green The selected green component
             * @param blue The selected blue component
             */
            @Override
            public void onColorPickerChanged(int red, int green, int blue) {
                Log.v(TAG, String.format("%d, %d, %d", red, green, blue));
                _connectedRobot.setLed(red, green, blue);
            }
        });

        // Find the color picker fragment and add a click listener to show the color picker
        _colorPickerButton = (Button)findViewById(R.id.colorPickerButton);
        _colorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.add(R.id.fragment_root, _colorPicker, "ColorPicker");
                transaction.show(_colorPicker);
                transaction.addToBackStack("DriveSample");
                transaction.commit();
            }
        });

    }

    private void setupDeveloperModeButton() {
        // Getting the developer mode button
        if (_developerModeLayout == null)
        {
            _developerModeSwitch = (Switch)findViewById(R.id.developerModeSwitch);
            _developerModeLayout = (LinearLayout)findViewById(R.id.developerModeLayout);

            _developerModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // We need to get the raw robot, as setting developer mode is an advanced function, and is not
                    // available on the Ollie object.
                    ((RobotLE)_connectedRobot.getRobot()).setDeveloperMode(isChecked);
                }
            });
        }
        _developerModeLayout.setVisibility(View.VISIBLE);
    }

    /**
     * Starts discovery on the set discovery agent and look for robots
     */
    private void startDiscovery() {
        try {
            // You first need to set up so that the discovery agent will notify you when it finds robots.
            // To do this, you need to implement the DiscoveryAgentEventListener interface (or declare
            // it anonymously) and then register it on the discovery agent with DiscoveryAgent#addDiscoveryListener()
            _currentDiscoveryAgent.addDiscoveryListener(this);
            // Second, you need to make sure that you are notified when a robot changes state. To do this,
            // implement RobotChangedStateListener (or declare it anonymously) and use
            // DiscoveryAgent#addRobotStateListener()
            _currentDiscoveryAgent.addRobotStateListener(this);
            // Then to start looking for a Sphero, you use DiscoveryAgent#startDiscovery()
            // You do need to handle the discovery exception. This can occur in cases where the user has
            // Bluetooth off, or when the discovery cannot be started for some other reason.
            _currentDiscoveryAgent.startDiscovery(this);
        } catch (DiscoveryException e) {
            Log.e(TAG, "Could not start discovery. Reason: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
