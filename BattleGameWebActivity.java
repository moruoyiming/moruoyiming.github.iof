package com.cocos.vs.battle.module.gameweb;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JsPromptResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.cocos.vs.battle.R;
import com.cocos.vs.core.socket.NetErrorCode;
import com.cocos.vs.core.utils.AudioUtils;
import com.cocos.vs.battle.utils.GameConstant;
import com.cocos.vs.core.unite.PropertiesUtils;
import com.cocos.vs.core.base.BaseApplication;
import com.cocos.vs.core.base.ui.BaseMVPActivity;
import com.cocos.vs.core.bean.GameListBean;
import com.cocos.vs.core.bean.LoginBean;
import com.cocos.vs.core.bean.cache.GameInfoCache;
import com.cocos.vs.core.bean.cache.GameSource;
import com.cocos.vs.core.bean.cache.UserInfoCache;
import com.cocos.vs.core.observer.setlement.SetlementDL;
import com.cocos.vs.core.protoc.ClientProto;
import com.cocos.vs.core.protoc.ClientProtoManager;
import com.cocos.vs.core.socket.SocketConstant;
import com.cocos.vs.core.socket.SocketManager;
import com.cocos.vs.core.socket.cache.SocketCache;
import com.cocos.vs.core.unite.log.VsLog;
import com.cocos.vs.core.unite.report.DataReportUtils;
import com.cocos.vs.core.unite.utils.ActivityStack;
import com.cocos.vs.core.utils.AudioServiceContext;
import com.cocos.vs.core.utils.CommonUtils;
import com.cocos.vs.core.utils.CoreConstant;
import com.cocos.vs.core.utils.CorePreferencesManager;
import com.cocos.vs.core.utils.MD5Utils;
import com.cocos.vs.core.utils.NetUtils;
import com.cocos.vs.core.utils.ScreenUtils;
import com.cocos.vs.core.utils.StatusBarUtils;
import com.cocos.vs.core.utils.ToastUtil;
import com.cocos.vs.core.utils.storage.FileUtils;
import com.cocos.vs.core.widget.customdialog.CustomDialog;
import com.cocos.vs.battle.bean.cpgame.FinishInfo;
import com.cocos.vs.battle.bean.cpgame.InitInfo;
import com.cocos.vs.battle.bean.cpgame.MessageInfo;
import com.cocos.vs.battle.bean.cpgame.ReadyInfo;
import com.cocos.vs.battle.bean.cpgame.RoomInfo;
import com.cocos.vs.battle.module.gamesettlement.GameSettlementActivity;
import com.cocos.vs.core.observer.VolumeChangeObserver;
import com.cocos.vs.core.utils.JsonParser;
import com.cocos.vs.interfacecore.ad.IAdResult;
import com.cocos.vs.interfacecore.voice.IVoiceResult;
import com.cocos.vs.interfacefactory.FactoryManage;
import com.xuhao.android.libsocket.sdk.client.ConnectionInfo;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import timber.log.Timber;

import static com.cocos.vs.core.unite.report.DataReportUtils.GAME_ID;
import static com.cocos.vs.core.unite.report.DataReportUtils.SOURCE;
import static com.cocos.vs.core.unite.report.DataReportUtils.VIVO_GAME_START_UP;
import static com.cocos.vs.core.protoc.ClientProto.CmdID.BATTLE_GAME_END_NOTICE;

/**
 * <pre>
 *     author: jian
 *     Date  : 8/2/18 2:45 PM
 *     Description: 对战游戏
 * </pre>
 */
@Route(path = "/module_battle/BattleGameWebActivity")
public class BattleGameWebActivity extends BaseMVPActivity<GamePresenter> implements IGameView, IAdResult, SocketManager.onGameBattleListener, VolumeChangeObserver.VolumeChangeListener, SocketManager.onSocketLoginListener, IVoiceResult {
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private WebView webView;
    private FrameLayout mFrame;
    private RelativeLayout mLayoutRoot;
    private RelativeLayout gameLoading;
    private TextView tvGameLoading;
    private ImageView ivMute;
    private ImageView ivSpeaker;
    private CustomDialog customDialog;
    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener;
    private VolumeChangeObserver mVolumeChangeObserver;
    private AudioManager mAudioManager;
    private AnimationDrawable soundAnim;
    private AnimationDrawable hornAnim;
    private boolean mGameOver = false;
    private boolean endNotice = false;
    private int lifecycleStatus = SocketConstant.STATUS_FOREGROUND;
    private int requestCode = -1;
    private int status = 1;
    private int userType;
    private int mRoomId;
    private int mRoomKey;
    private int mGameId;
    private int opUserId;
    private String gameVersion;
    private String mGameMD5;
    private String url;
    private String opSex;
    private String opName;
    private String opHeadUrl;
    private static final int DELAYED_UPDATE_LOADING_TEXT = 6;
    private static final int DELAYED_TIME = 6000;
//    private String publisherId = "875434275-096F7E-DC29-61DA-3889C0929";
//    private String mBannerAdId = "875434275pld85f";
//    private String mInterstitialAdId = "875434275pld8ba";
//    private String mVideoAdId = "875434275pld9dt";

    @Override
    public void bindView() {
        mFrame = (FrameLayout) findViewById(R.id.web_parent);
        gameLoading = (RelativeLayout) findViewById(R.id.rl_game_loading);
        mLayoutRoot = findViewById(R.id.rv_root);
        tvGameLoading = (TextView) findViewById(R.id.tv_tips);
        //        webView=findViewById(R.id.web_view);
        findViewById(R.id.iv_back).setOnClickListener(mOnClickListener);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (customDialog != null && !customDialog.isShowing()) {
                customDialog.show();
            }
        }
    };

    @Override
    public void init() {
        SocketManager.getInstance().setOnGameBattleListener(this);
        SocketManager.getInstance().setOnSocketLoginListener(this);
        initView();
        initAudioSpeaker();
        initData();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            presenter.initAgoraEngine(BattleGameWebActivity.this);
            presenter.joinChannel(mRoomId, UserInfoCache.getInstance().getUserInfo().getUserId());
        }
        initCacheSocket();
        showLoading(true);
        //游戏初始化
        FactoryManage.getInstance().getStatisticsFactory().gameInit(this, String.valueOf(UserInfoCache.getInstance().getUserInfo().getUserId()), String.valueOf(mGameId));
    }

    /**
     * 解析 H5 请求方法
     *
     * @param cmd
     * @param param
     */
    public void interpretingData(String cmd, String param) {
        try {
            if (endNotice || isFinishing()) {  // 发现gameEnd之后，仍然走sendBegin
                return;
            }
            JSONObject jsonObject = null;
            if (!TextUtils.isEmpty(param)) {
                jsonObject = new JSONObject(param);
            }
            switch (cmd) {
                /************** CP游戏无服务器 **************/
                case GameConstant.JS_GAME_READY://游戏准备 userData: string 用户数据
                    String userData = JsonParser.getString(jsonObject, "userData");
                    SocketConstant.sendMessage(ClientProtoManager.sendBattleReady(mRoomId, userData));
                    break;
                case GameConstant.JS_GAME_BROADCAST://游戏消息广播  message: string 广播的消息 includeMe: int 是否也广播给自己: 0、不包含，1、包含
                    String message = JsonParser.getString(jsonObject, "message");
                    int includeMe = JsonParser.getInt(jsonObject, "includeMe");
                    SocketConstant.sendMessage(ClientProtoManager.sendBattleBroadcast(mRoomId, message, includeMe == 0 ? false : true));
                    break;
                case GameConstant.JS_GAME_GAMEOVER://游戏结束 result: int 游戏结果: 1、胜，2、负，3、平
                {
                    int result = JsonParser.getInt(jsonObject, "result");
                    SocketConstant.sendMessage(ClientProtoManager.sendGameOver(mRoomId, ClientProto.GameResult.valueOf(result)));
                }
                break;
                case GameConstant.JS_QUIT://退出游戏 reason: int 退出原因: 1 - 正常退出，2-异常退出
                    finish();
                    break;
                /************** CP游戏有服务器 **************/
                case GameConstant.JS_INIT:
                    int version = JsonParser.getInt(jsonObject, "version");
                    int gameId = JsonParser.getInt(jsonObject, "gameId");
                    String gameKey = JsonParser.getString(jsonObject, "gameKey");
                    String gameSecret = JsonParser.getString(jsonObject, "gameSecret");
                    String gameMd5 = MD5Utils.getStringMD5(gameId + gameKey + gameSecret);
                    if (CoreConstant.TEST_APP) {
                        mGameMD5 = "gameSecret";
                        gameMd5 = "gameSecret";
                    }
                    FactoryManage.getInstance().getAdFactory().init(BattleGameWebActivity.this);
                    FactoryManage.getInstance().getAdFactory().setLoadAdResult(this);
                    if (!TextUtils.isEmpty(mGameMD5) && TextUtils.equals(gameMd5, mGameMD5)) {
                        InitInfo initInfo = new InitInfo();
                        initInfo.setError(0);//校验成功 0
                        String initInfoJson = BaseApplication.getGson().toJson(initInfo);
                        CallJSMethod(GameConstant.NATIVE_ONINIT, initInfoJson);
                        SocketConstant.sendMessage(ClientProtoManager.sendGameBegin(mRoomId));
                        if (lifecycleStatus == SocketConstant.STATUS_FOREGROUND) {
                            CallJSMethod(GameConstant.NATIVE_ONRESUME, SocketConstant.onGameResume());
                            CallJSMethod(GameConstant.NATIVE_ONAUDIO, SocketConstant.playGameAudio(100));
                        }
                    } else {
                        ToastUtil.showCenterToast(R.string.vs_battle_game_error);
                    }
                    if (version <= GameConstant.GAME_VERSION_ONE) {
                        dismissLoading();
                    }
                    presenter.adUnitPosition(GameConstant.AD_BANNER, "875434275", new HashMap<String, Object>());
                    presenter.adUnitPosition(GameConstant.AD_INTERSTITIAL, "875434276", new HashMap<String, Object>());
                    presenter.adUnitPosition(GameConstant.AD_VIDEO, "875434277", new HashMap<String, Object>());
                    break;
                case GameConstant.JS_ROOMINFO:
                    RoomInfo roomInfo = new RoomInfo();
                    roomInfo.setError(0);
                    roomInfo.setGameId(mGameId);
                    roomInfo.setRoomId(mRoomId);
                    roomInfo.setRoomKey(mRoomKey);
                    List<RoomInfo.User> uesrs = new ArrayList<>();
                    RoomInfo.User play1 = new RoomInfo.User();
                    LoginBean loginBean = UserInfoCache.getInstance().getUserInfo();
                    play1.setNickname(loginBean.getNickName());
                    play1.setHeadUrl(loginBean.getPhotoUrl());
                    play1.setUserId(loginBean.getUserId());
                    play1.setSex(loginBean.getSex());
                    play1.setType(1);
                    uesrs.add(play1);
                    RoomInfo.User play2 = new RoomInfo.User();
                    play2.setNickname(opName);
                    play2.setHeadUrl(opHeadUrl);
                    play2.setUserId(opUserId);
                    play2.setSex(opSex);
                    play2.setType(userType);
                    uesrs.add(play2);
                    roomInfo.setUsers(uesrs);
                    String roomInfoJson = BaseApplication.getGson().toJson(roomInfo);
                    CallJSMethod(GameConstant.NATIVE_ONROOMINFO, roomInfoJson);
                    break;
                case GameConstant.JS_FINISH://result // 结果:1、胜;2、负;3、平
                    if (!mGameOver) {
                        mGameOver = true;
                        int result = JsonParser.getInt(jsonObject, "result");
                        status = result;
                        SocketConstant.sendMessage(ClientProtoManager.sendGameEnd(mRoomId, ClientProto.GameEndReason.GAME_END, ClientProto.GameResult.valueOf(result)));
                    }
                    break;
                /************** 公用  **************/
                case GameConstant.JS_SETORIENTATION:// 朝向: 0 横屏;1竖屏
                    int orientation = JsonParser.getInt(jsonObject, "orientation");
                    if (orientation == 0) {//设置横屏
                        ScreenUtils.setLandscape(this);
                    } else {//设置竖屏
                        ScreenUtils.setPortrait(this);
                    }
                    break;
                case GameConstant.JS_SETAUDIO://int 0:关闭， 1: 打开
                    int enable = JsonParser.getInt(jsonObject, "enable");
                    int volume = JsonParser.getInt(jsonObject, "volume");
                    break;
                case GameConstant.JS_SETMIC://int 0:关闭， 1: 打开
                    int enable2 = JsonParser.getInt(jsonObject, "enable");
                    break;
                case GameConstant.NATIVE_LOADPROGRESS:
                    int progress = JsonParser.getInt(jsonObject, "progress");
                    if (progress >= 100) {
                        updateLoadingText(getResources().getString(R.string.vs_battle_opponent_loading_text));
                        myHandler.sendEmptyMessageDelayed(DELAYED_UPDATE_LOADING_TEXT, DELAYED_TIME);
                    }
                    break;
                case GameConstant.NATIVE_HIDELOAD:
                    dismissLoading();
                    break;
                case GameConstant.JS_AD_BANNER_CREATE: {
                    String mBannerAdId = JsonParser.getString(jsonObject, "bannerAdId");
                    String adId = JsonParser.getString(jsonObject, "adId");
                    int position = JsonParser.getInt(jsonObject, "position");
                    HashMap<String, Object> params = new HashMap<>();
                    params.put("adId", adId);
                    params.put("position", position);
                    presenter.adUnitPosition(GameConstant.AD_BANNER, mBannerAdId, params);
//                    FactoryManage.getInstance().getAdFactory().createBannerAd(GameConstant.publisherId, GameConstant.positionId, adId, position, mFrame);
                }
                break;
                case GameConstant.JS_AD_BANNER_SHOW: {
                    String adId = JsonParser.getString(jsonObject, "adId");
                    FactoryManage.getInstance().getAdFactory().showAd(CoreConstant.AD_BANNER, adId);
                }
                break;
                case GameConstant.JS_AD_BANNER_HIDE: {
                    String adId = JsonParser.getString(jsonObject, "adId");
                    FactoryManage.getInstance().getAdFactory().hideAd(CoreConstant.AD_BANNER, adId);
                }
                break;
                case GameConstant.JS_AD_BANNER_DESTROY: {
                    String adId = JsonParser.getString(jsonObject, "adId");
                    FactoryManage.getInstance().getAdFactory().destroy(CoreConstant.AD_BANNER, adId);
                }
                break;
                case GameConstant.JS_AD_INTERSTITIAL_CREATE: {
                    String mInterstitialAdId = JsonParser.getString(jsonObject, "interstitialAdId");
                    String adId = JsonParser.getString(jsonObject, "adId");
                    int style = JsonParser.getInt(jsonObject, "style");
                    HashMap<String, Object> params = new HashMap<>();
                    params.put("adId", adId);
                    params.put("style", style);
                    presenter.adUnitPosition(GameConstant.AD_BANNER, mInterstitialAdId, params);
//                    FactoryManage.getInstance().getAdFactory().createInterstitialAd(GameConstant.publisherId, GameConstant.positionId, adId, style);
                }
                break;
                case GameConstant.JS_AD_INTERSTITIAL_SHOW: {
                    String adId = JsonParser.getString(jsonObject, "adId");
                    FactoryManage.getInstance().getAdFactory().showAd(CoreConstant.AD_INTERSTITIAL, adId);
                }
                break;
                case GameConstant.JS_AD_INTERSTITIAL_HIDE: {
                    String adId = JsonParser.getString(jsonObject, "adId");
                    FactoryManage.getInstance().getAdFactory().hideAd(CoreConstant.AD_INTERSTITIAL, adId);
                }
                break;
                case GameConstant.JS_AD_INTERSTITIAL_DESTROY: {
                    String adId = JsonParser.getString(jsonObject, "adId");
                    FactoryManage.getInstance().getAdFactory().destroy(CoreConstant.AD_INTERSTITIAL, adId);
                }
                break;
                case GameConstant.JS_AD_REWARDEDVIDEO_CREATE: {
                    String mVideoAdId = JsonParser.getString(jsonObject, "videoAdId");
                    String adId = JsonParser.getString(jsonObject, "adId");
                    int screenorientation = JsonParser.getInt(jsonObject, "screenOrientation");
                    HashMap<String, Object> params = new HashMap<>();
                    params.put("adId", adId);
                    params.put("screenorientation", screenorientation);
                    presenter.adUnitPosition(GameConstant.AD_BANNER, mVideoAdId, params);
//                    FactoryManage.getInstance().getAdFactory().createVideoAd(GameConstant.publisherId, GameConstant.positionId, adId, screenorientation);
                }
                break;
                case GameConstant.JS_AD_REWARDEDVIDEO_SHOW: {
                    String adId = JsonParser.getString(jsonObject, "adId");
                    FactoryManage.getInstance().getAdFactory().showAd(CoreConstant.AD_VIDEO, adId);
                }
                case GameConstant.JS_AD_REWARDEDVIDEO_LOAD: {
                    String adId = JsonParser.getString(jsonObject, "adId");
                    FactoryManage.getInstance().getAdFactory().hideAd(CoreConstant.AD_VIDEO, adId);
                }
                break;

            }
        } catch (Exception e) {
            Timber.tag("JS->NATIVE").i("JS call Native Exception %s ", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onBattleEnterAck(int code) {
        if (code >= NetErrorCode.BATTLE_ROOM_NOT_EXISTS && code <= NetErrorCode.BATTLE_ROOM_GAME_END) {
            gameFinish(status);
            Timber.tag(TAG).i(" fail!!" + code);
        }
    }

    @Override // 玩家进入游戏发送ready之后 服务器返回GameReadyNotice 和 GameStartNotice
    public void onBattleGameReadyNotice(int userId, String userData) {
        Timber.tag("BattleGame").i("onBattleGameReadyNotice userId = " + userId + "   userData = " + userData);
        ReadyInfo readyInfo = new ReadyInfo();
        readyInfo.setUserId(userId);
        readyInfo.setUserData(SocketConstant.stringToJson(userData));
        String readyinfoJson = BaseApplication.getGson().toJson(readyInfo);
        CallJSMethod(GameConstant.NATIVE_ONREADY, readyinfoJson);
    }

    @Override
    public void onBattleGameStartNotice() {
        Timber.tag("BattleGame").i("onBattleGameStartNotice  ");
        CallJSMethod(GameConstant.NATIVE_ONSTART, "{}");
    }

    @Override //收到服务器消息并且将消息发送给游戏
    public void onBattleGameMessageNotice(int userId, String message) {
        Timber.tag("BattleGame").i("onBattleGameMessageNotice userId = " + userId + "   message = " + message);
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setUserId(userId);
        messageInfo.setMessage(SocketConstant.stringToJson(message));
        String messageInfoJson = BaseApplication.getGson().toJson(messageInfo);
        CallJSMethod(GameConstant.NATIVE_ONMESSAGE, messageInfoJson);
    }

    @Override
    public void onBattleGameOverNotice(ClientProto.GameResult result) {
        Timber.tag("BattleGame").i("onBattleGameOverNotice result = %s ", result.getNumber());
        FinishInfo finishInfo = new FinishInfo();
        finishInfo.setResult(result.getNumber());
        CallJSMethod(GameConstant.NATIVE_ONFINISH, BaseApplication.getGson().toJson(finishInfo));
    }

    @Override
    public void onBattleGameEndNotice(final int result, int reason) {
        endNotice = true;
        if (reason == ClientProto.GameEndReason.GAME_END_VALUE) {
            gameFinish(result);
        } else if (reason == ClientProto.GameEndReason.FORCE_END_VALUE) {
            showToast(getString(R.string.vs_battle_op_exit_game), 1000);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    gameFinish(result);
                }
            }, 1200);
        }
    }


    @Override
    public void onBattleLeaveNotice(int userId, int battleLeaveReason) {

    }

    public void initView() {
        // 无用户操作时，游戏页保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ScreenUtils.setFullScreen(this);
        webView = new WebView(this);
        mFrame.addView(webView);
        // Todo 找到WebView初始化时覆盖DisplayMetrics的原因
        // BaseMVPActivity中设置的属性，会被webView = new WebView覆盖暂未找到覆盖的原因，先重新调用覆盖
        setCustomDensity(this, getApplication());
        updateViewOnConfigChanged();
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient() {
            @Nullable
            @Override //TODO 解决白鹭引擎 无法加载本地文件
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                WebResourceResponse resourceResponse = null;
                try {
                    if (url.startsWith("file:///")) {
                        Uri uri = Uri.parse(url);
                        String filePath = uri.getPath();
                        String exName = SocketConstant.getExtensionName(filePath);
                        String mimeType = PropertiesUtils.readProperty(exName);
                        if (mimeType != null) {
                            Timber.tag("shouldInterceptRequest").i("file name %s , mimeType  %s.", exName, mimeType);
                            InputStream localFile = new FileInputStream(filePath);
                            resourceResponse = new WebResourceResponse(mimeType, "UTF-8", localFile);
                            return resourceResponse;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                Uri uri = Uri.parse(message);
                if (uri.getScheme().equals(GameConstant.JS)) {// "js://callNative?cmd="+cmd+"&param="+param;
                    if (uri.getAuthority().equals(GameConstant.JS_METHOD)) {
                        String cmdValue = uri.getQueryParameter(GameConstant.JS_FEILD);
                        String paramValue = uri.getQueryParameter(GameConstant.JS_PARAM);
                        Timber.tag("JS->NATIVE").i("JS call Native cmd =  %s , param=   %s.", cmdValue, paramValue);
                        interpretingData(cmdValue, paramValue);
                        result.confirm("JS call Native method succeful ~~~");
                    }
                    return true;
                }
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Timber.tag("Console").i(" console message = %s ", consoleMessage);
                return super.onConsoleMessage(consoleMessage);
            }
        });
        customDialog = new CustomDialog.Builder(BattleGameWebActivity.this)
                .setTitle(getString(R.string.vs_prompt))
                .setMessage(getString(R.string.vs_quick_game))
                .setNegativeButton(getString(R.string.vs_continue_play), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        customDialog.dismiss();
                    }
                })
                .setPositiveButton(getString(R.string.vs_defeat_exit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SocketManager.getInstance().removeGameBattleListener();
                        SocketManager.getInstance().removeSocketConnectionListener();
                        SocketConstant.sendMessage(ClientProtoManager.sendGameEnd(mRoomId, ClientProto.GameEndReason.FORCE_END, ClientProto.GameResult.LOSE));
                        if (requestCode == GameConstant.REQUEST_CODE_CHAT) {
                            SetlementDL.getInstance().notifyObserver(GameConstant.FIGHT_FAIL, mRoomId);
                        } else {
                            GameSettlementActivity.toGameSettlementActivity(BattleGameWebActivity.this, mRoomId, mGameId, GameConstant.FIGHT_FAIL, opName, opSex, opHeadUrl, opUserId, userType);
                        }
                        finish();
                    }
                })
                .create();
        //实例化对象并设置监听器
        mVolumeChangeObserver = new VolumeChangeObserver(getApplicationContext());
        mVolumeChangeObserver.setVolumeChangeListener(this);
        int initVolume = mVolumeChangeObserver.getCurrentMusicVolume();
        VsLog.d(TAG, "initVolume = " + initVolume);
        ivMute = (ImageView) findViewById(R.id.iv_mute);
        ivMute.setSelected(true);
        ivMute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView iv = (ImageView) v;
                if (iv.isSelected()) {
                    iv.setSelected(false);
                    ivMute.setImageDrawable(soundAnim);
                    if (soundAnim != null && !soundAnim.isRunning()) {
                        soundAnim.start();
                    }
                } else {
                    iv.setSelected(true);
                    if (soundAnim != null && soundAnim.isRunning()) {
                        soundAnim.stop();
                    }
                    ivMute.setImageResource(R.drawable.vs_battle_sound_close);
                }
                presenter.muteLocalAudioStream(iv.isSelected());
            }
        });
        ivSpeaker = (ImageView) findViewById(R.id.iv_speaker);
        ivSpeaker.setSelected(true);
        ivSpeaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageView iv = (ImageView) v;
                if (iv.isSelected()) {
                    iv.setSelected(false);
                    ivSpeaker.setImageDrawable(hornAnim);
                    if (hornAnim != null && !hornAnim.isRunning()) {
                        hornAnim.start();
                    }
                } else {
                    iv.setSelected(true);
                    if (hornAnim != null && hornAnim.isRunning()) {
                        hornAnim.stop();
                    }
                    ivSpeaker.setImageResource(R.drawable.vs_battle_horn_close);
                }
                presenter.muteRemoteAudioStream(opUserId, iv.isSelected());
            }
        });
    }


    public void initData() {
        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            requestCode = intent.getIntExtra(GameConstant.REQUEST_ACTION, -1);
            mRoomId = intent.getIntExtra(GameConstant.ROOMID_ACTION, 0);
            mGameId = intent.getIntExtra(GameConstant.GAMEID_ACTION, 0);
            mRoomKey = intent.getIntExtra(GameConstant.ROOMKEY_ACTION, 0);
            userType = intent.getIntExtra(GameConstant.USERTYPE_ACTION, 0);
            opUserId = intent.getIntExtra(GameConstant.USERID_ACTION, 0);
            opName = intent.getStringExtra(GameConstant.NICKNAME_ACTION);
            opHeadUrl = intent.getStringExtra(GameConstant.HEADURL_ACTION);
            opSex = intent.getStringExtra(GameConstant.SEX_ACTION);
            url = intent.getStringExtra(GameConstant.GAMEURL_ACTION);
            GameListBean.GameInfo gameInfo = GameInfoCache.getInstance().getGameInfo(mGameId);
            if (gameInfo != null) {
                gameVersion = gameInfo.getGameVer();
                mGameMD5 = gameInfo.getGameValidate();
                if (NetUtils.isNetWorkAvaliable()) {
                    if (url.equals(FileUtils.starGameIndexHtml(mGameId, gameVersion))) {
                        Timber.tag("webView").i(" BattleGameWebActivity webView loadUrl url = %s ", url);
                        webView.loadUrl(url);
                    }
                }
            }
            dataReport(String.valueOf(mGameId));
        }

    }

    public void initAudioSpeaker() {
        soundAnim = (AnimationDrawable) getResources().getDrawable(R.drawable.vs_battle_sound_animation);
        hornAnim = (AnimationDrawable) getResources().getDrawable(R.drawable.vs_battle_horn_animation);
    }

    /**
     * 调用H5 方法
     *
     * @param method sum
     * @param param  1,2
     */
    public void CallJSMethod(String method, String param) {
        String what = GameConstant.JS_METHOD_CB + "('" + method + "','" + param + "')";
        Timber.tag("NATIVE->JS").i("Native call JS cmd= %s , param=  %s. , what =  %s.", method, param, what);
        if (webView != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                webView.loadUrl("javascript:" + what);//+ method + "(" + param + ")"
            } else {
                webView.evaluateJavascript(what, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        Timber.tag("CallJSMethod").i("Call JS method succeful , return data is = %s.", value);
                    }
                });
            }
        }
    }

    private void initCacheSocket() {
        if (SocketCache.getInstance().getSocketCache(BATTLE_GAME_END_NOTICE.getNumber()) != null && !TextUtils.isEmpty(SocketCache.getInstance().getSocketCache(BATTLE_GAME_END_NOTICE.getNumber()))) {
            onBattleGameEndNotice(Integer.valueOf(SocketCache.getInstance().getSocketCache(BATTLE_GAME_END_NOTICE.getNumber())), ClientProto.GameEndReason.GAME_END_VALUE);
        }
    }

    private void dataReport(String gameId) {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put(GAME_ID, gameId);
        hashMap.put(SOURCE, GameSource.getInstance().getSource());
        DataReportUtils.onSingleImmediateEvent(VIVO_GAME_START_UP, hashMap);
    }

    public static void toBattleGameWebActivity(Activity context, String url, int opUserId, String opNickName,
                                               String opHeadUrl, String opSex, int roomId, int roomKey, int gameId, int userType, int requestCode) {
        Bundle bundle = new Bundle();
        bundle.putString(GameConstant.GAMEURL_ACTION, url);
        bundle.putString(GameConstant.HEADURL_ACTION, opHeadUrl);
        bundle.putString(GameConstant.NICKNAME_ACTION, opNickName);
        bundle.putString(GameConstant.SEX_ACTION, opSex);
        bundle.putInt(GameConstant.USERID_ACTION, opUserId);
        bundle.putInt(GameConstant.GAMEID_ACTION, gameId);
        bundle.putInt(GameConstant.ROOMID_ACTION, roomId);
        bundle.putInt(GameConstant.USERTYPE_ACTION, userType);
        bundle.putInt(GameConstant.ROOMKEY_ACTION, roomKey);
        Intent intent = new Intent(context, BattleGameWebActivity.class);
        intent.putExtras(bundle);
        intent.putExtra(GameConstant.REQUEST_ACTION, requestCode);
        context.startActivityForResult(intent, requestCode);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
        if (lifecycleStatus == SocketConstant.STATUS_BACKGROUND) {
            CallJSMethod(GameConstant.NATIVE_ONRESUME, SocketConstant.onGameResume());
            CallJSMethod(GameConstant.NATIVE_ONAUDIO, SocketConstant.playGameAudio(100));
            lifecycleStatus = SocketConstant.STATUS_FOREGROUND;
        }
        mVolumeChangeObserver.registerReceiver();
        if (CorePreferencesManager.getVoice() && requestTheAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            AudioUtils.getSystemVolumeSize(getApplicationContext(), true);
            AudioUtils.mute(getApplicationContext());
        }
        if (mGameOver) {
            gameFinish(status);
            SocketManager.getInstance().removeGameBattleListener();
            SocketManager.getInstance().removeSocketConnectionListener();
        }
        FactoryManage.getInstance().getStatisticsFactory().onResume(this);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AudioServiceContext.getContext(base));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
        CallJSMethod(GameConstant.NATIVE_ONPAUSE, SocketConstant.onGamePause());
        CallJSMethod(GameConstant.NATIVE_ONAUDIO, SocketConstant.pauseGameAudio());
        lifecycleStatus = SocketConstant.STATUS_BACKGROUND;
        releaseTheAudioFocus(mAudioFocusChangeListener);
        FactoryManage.getInstance().getStatisticsFactory().onPause(this);
    }


    @Override
    public void onStop() {
        super.onStop();
        if (isFinishing()) {
            destroy();
        }
        SocketCache.getInstance().setSocketCache(BATTLE_GAME_END_NOTICE.getNumber(), "");
        AudioUtils.unmute(getApplicationContext());
    }

    private void destroy() {
        mVolumeChangeObserver.unregisterReceiver();
        if (webView != null) {
            webView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            webView.clearHistory();
            mFrame.removeView(webView);
            webView.destroy();
            webView = null;
        }
        if (customDialog != null && customDialog.isShowing()) {
            customDialog.dismiss();
            customDialog = null;
        }
        if (mVolumeChangeObserver != null) {
            mVolumeChangeObserver.setVolumeChangeListener(null);
            mVolumeChangeObserver = null;
        }
        if (mAudioFocusChangeListener != null) {
            mAudioFocusChangeListener = null;
        }
        if (mAudioManager != null) {
            mAudioManager = null;
        }
    }

    @Override
    public void onDestroy() {
        presenter.leaveChannel(mRoomId, UserInfoCache.getInstance().getUserInfo().getUserId());
        presenter.destroy();
        myHandler.removeMessages(DELAYED_UPDATE_LOADING_TEXT);
        FactoryManage.getInstance().getStatisticsFactory().gameLogout(this);
        super.onDestroy();
    }

    /**
     * 菜单、返回键响应
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (customDialog != null && !customDialog.isShowing()) {
                    customDialog.show();
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (mAudioManager != null) {
                    mAudioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_RAISE,
                            AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                    VsLog.i("VolumeSize", "声音大小--->增加");
                    return true;
                }

            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (mAudioManager != null) {
                    mAudioManager.adjustStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_LOWER,
                            AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                    VsLog.i("VolumeSize", "声音大小--->减少");
                    return true;
                }
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        }
    }

    // 请求音频焦点 设置监听
    private int requestTheAudioFocus() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        if (mAudioFocusChangeListener == null) {
            mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {//监听器
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                            //播放操作
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            //暂停操作
                            break;
                        default:
                            break;
                    }
                }
            };
        }
        //下面两个常量参数试过很多 都无效，最终反编译了其他app才搞定，汗~
        int requestFocusResult = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        return requestFocusResult;
    }

    // 暂停、播放完成或退到后台释放音频焦点
    private void releaseTheAudioFocus(AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener) {
        if (mAudioManager != null && mAudioFocusChangeListener != null) {
            mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
        }
    }

    @Override
    public void onVolumeChanged(int volume) {
        VsLog.i("VolumeSize", "onVolumeChanged()--->volume = " + volume);
        AudioUtils.getSystemVolumeSize(getApplicationContext(), false);

    }


    /**
     * 结束游戏
     *
     * @param result
     */
    public void gameFinish(int result) {
        endNotice = true;//TODO 发现gameEnd之后，仍然走sendBegin
        if (lifecycleStatus == SocketConstant.STATUS_FOREGROUND) {//在前台发送gameend结束消息
            SocketManager.getInstance().removeGameBattleListener();
            SocketManager.getInstance().removeSocketConnectionListener();
            SocketManager.getInstance().removeSocketLoginListener();
            if (requestCode == GameConstant.REQUEST_CODE_CHAT) {
                SetlementDL.getInstance().notifyObserver(result, mRoomId);
            } else {
                GameSettlementActivity.toGameSettlementActivity(BattleGameWebActivity.this, mRoomId, mGameId, result, opName, opSex, opHeadUrl, opUserId, userType);
            }
            finish();
        } else {
            mGameOver = true;
        }
    }

    @Override
    public int getActivityTag() {
        return ActivityStack.TAG_ACCOUNT_LOGOUT;
    }


    @Override
    protected int provideContentViewId() {
        return R.layout.vs_battle_activity_web;
    }

    @Override
    protected GamePresenter getPresenter() {
        return new GamePresenter(this, this);
    }

    @Override
    public void dismissLoading() {
        myHandler.removeMessages(DELAYED_UPDATE_LOADING_TEXT);
        showLoading(false);
    }

    @Override
    public void onUserJoined(int uid, int elapsed) {

    }

    @Override
    public void onUserOffline(int uid, int reason) {

    }

    @Override
    public void onUserMuteAudio(int uid, boolean muted) {

    }

    @Override
    public void onJoinRoomSuccess(String roomId, int uid, int elapsed) {

    }

    public void showLoading(boolean isShow) {
        if (isShow) {
            gameLoading.setVisibility(View.VISIBLE);
        } else {
            gameLoading.setVisibility(View.GONE);
        }
    }

    public void updateLoadingText(String loadingText) {
        if (tvGameLoading != null) {
            tvGameLoading.setText(loadingText);
        }
    }

    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case DELAYED_UPDATE_LOADING_TEXT:
                    updateLoadingText(BattleGameWebActivity.this.getResources().getString(R.string.vs_battle_opponent_loading_slow_text));
                    break;
            }
        }
    };

    public boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            PermissionRequest();
            return false;
        }
        return true;
    }

    /**
     * 权限申请
     */
    public void PermissionRequest() {
        AndPermission.with(this)
                .runtime()
                .permission(Permission.Group.MICROPHONE)
                .onGranted(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {
                        presenter.initAgoraEngine(BattleGameWebActivity.this);
                        presenter.joinChannel(mRoomId, UserInfoCache.getInstance().getUserInfo().getUserId());
                    }
                })
                .onDenied(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {
                        CustomDialog mCustomDialog = new CustomDialog.Builder(BattleGameWebActivity.this)
                                .setTitle(getString(R.string.vs_prompt))
                                .setMessage(getString(R.string.vs_battle_permission_audio))
                                .setNegativeButton(getString(R.string.vs_constant_canncel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton(getString(R.string.vs_go_setting), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, 1);
                                        dialog.dismiss();
                                    }
                                })
                                .create();
                        mCustomDialog.show();
                    }
                })
                .start();
    }

    @Override
    public void onLoginAck(String message) {
        if (SocketConstant.LOGIN_STATUS == SocketConstant.LOGIN_STATUS_SUCCEFUL) {
            SocketConstant.sendBattleEnter(mRoomId, mRoomKey);
        }
    }

    @Override
    public void onLogoutAck(String message) {

    }

    @Override
    public void onLoad(int type, String adId) {
        HashMap<String, String> what = new HashMap<>();
        what.put("adId", adId);
        String params = BaseApplication.getGson().toJson(what);
        switch (type) {
            case CoreConstant.AD_BANNER:
                CallJSMethod(GameConstant.NATIVE_AD_BANNER_ONLOAD, params);
                break;
            case CoreConstant.AD_INTERSTITIAL:
                CallJSMethod(GameConstant.NATIVE_AD_INTERSTITIAL_ONLOAD, params);
                break;
            case CoreConstant.AD_VIDEO:
                CallJSMethod(GameConstant.NATIVE_AD_REWARDEDVIDEO_ONLOAD, params);
                break;
        }
    }

    @Override
    public void onError(int type, String adId, String message) {
        HashMap<String, String> what = new HashMap<>();
        what.put("adId", adId);
        String params = BaseApplication.getGson().toJson(what);
        switch (type) {
            case CoreConstant.AD_BANNER:
                CallJSMethod(GameConstant.NATIVE_AD_BANNER_ONERROR, params);
                break;
            case CoreConstant.AD_INTERSTITIAL:
                CallJSMethod(GameConstant.NATIVE_AD_INTERSTITIAL_ONERROR, params);
                break;
            case CoreConstant.AD_VIDEO:
                CallJSMethod(GameConstant.NATIVE_AD_REWARDEDVIDEO_ONERROR, params);
                break;
        }
    }

    @Override
    public void showAdSuccess(int type, String adId) {
        HashMap<String, String> what = new HashMap<>();
        what.put("type", String.valueOf(type));
        what.put("adId", adId);
        what.put("success", "0");
        String params = BaseApplication.getGson().toJson(what);
        switch (type) {
            case CoreConstant.AD_BANNER:
                CallJSMethod(GameConstant.AD_ONSHOW, params);
                break;
            case CoreConstant.AD_INTERSTITIAL:
                CallJSMethod(GameConstant.AD_ONSHOW, params);
                break;
            case CoreConstant.AD_VIDEO:
                CallJSMethod(GameConstant.AD_ONSHOW, params);
                break;
        }
    }

    @Override
    public void hideAdSuccess(int type, String adId) {
//        HashMap<String, String> what = new HashMap<>();
//        what.put("type", String.valueOf(type));
//        what.put("adId", adId);
//        what.put("success", "0");
//        String appId = BaseApplication.getGson().toJson(what);
//        switch (type) {
//            case CoreConstant.AD_BANNER:
//                CallJSMethod(GameConstant.AD_ONHIDE, appId);
//                break;
//            case CoreConstant.AD_INTERSTITIAL:
//                CallJSMethod(GameConstant.AD_ONHIDE, appId);
//                break;
//            case CoreConstant.AD_VIDEO:
//                CallJSMethod(GameConstant.AD_ONHIDE, appId);
//                break;
//        }
    }

    @Override
    public void closeVideoAd(int type, String adId) {
//        HashMap<String, String> what = new HashMap<>();
//        what.put("type", String.valueOf(type));
//        what.put("adId", adId);
//        String params = BaseApplication.getGson().toJson(what);
//        CallJSMethod(GameConstant.NATIVE_AD_REWARDEDVIDEO_ONCLOSE, params);
    }

    @Override
    public void createBannerAd(String publisherId, String positionId, String adId, int position) {
        FactoryManage.getInstance().getAdFactory().createBannerAd(publisherId,positionId, adId, position, mFrame);
    }

    @Override
    public void createInterstitialAd(String publisherId, String positionId, String adId, int style) {
        FactoryManage.getInstance().getAdFactory().createInterstitialAd(GameConstant.publisherId, GameConstant.positionId, adId, style);
    }

    @Override
    public void createVideoAd(String publisherId, String positionId, String adId, int screenorientation) {
        FactoryManage.getInstance().getAdFactory().createVideoAd(GameConstant.publisherId, GameConstant.positionId, adId, screenorientation);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        if (!isInMultiWindowMode) {
            recoverLayoutForNonNormalUI();
        }
    }

    /**
     * 适配分屏
     * 分屏状态下，使得游戏页面可以完全显示
     */
    private void initLayoutForNonNormalUI() {
        if (mLayoutRoot == null || !CommonUtils.shouldHandleNonNormalUI(this)) {
            return;
        }
        mLayoutRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                float value = displayMetrics.widthPixels - displayMetrics.heightPixels * 9 / 16f;
                if (value > 0 && mLayoutRoot != null) {
                    mLayoutRoot.setPadding((int) (value / 2), 0, (int) (value / 2), 0);
                }
                mLayoutRoot.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

    }

    private void recoverLayoutForNonNormalUI() {
        if (mLayoutRoot != null) {
            mLayoutRoot.setPadding(0, 0, 0, 0);
        }
    }

    public void updateViewOnConfigChanged() {
        if (webView == null) {
            return;
        }
        if (!StatusBarUtils.hasNavigationBar(this)) {
            return;
        }
        int height = StatusBarUtils.getVirtualBarHeigh(this);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int left = 0;
        int right = 0;
        int top = 0;
        int bottom = 0;
        switch (rotation) {
            case Surface.ROTATION_270:
                left = height;
                right = 0;
                bottom = 0;
                break;
            case Surface.ROTATION_90:
                left = 0;
                right = height;
                bottom = 0;
                break;
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                left = 0;
                right = 0;
                bottom = height;
                break;
            default:
                break;
        }

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(webView.getLayoutParams());
        lp.setMargins(left, top, right, bottom);
        webView.setLayoutParams(lp);
    }
}

