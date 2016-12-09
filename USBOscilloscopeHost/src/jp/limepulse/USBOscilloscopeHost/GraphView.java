package jp.limepulse.USBOscilloscopeHost;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.util.Log;
import android.view.View;
import jp.limepulse.USBOscilloscopeHost.R;

public class GraphView extends View {
	private static final String TAG = "Graph";
	private static final boolean D = false;


	private int FrameRate = 0;

	private double width;
	private double height;

	private final Paint p_wave = new Paint();	//�g�`
	private final Paint p_waku = new Paint();	//�g
	private final Paint p_masu = new Paint(); //�}�X��
	private final Paint p_HTriggerDeep = new Paint(); // �����g���K�[�Z��
	private final Paint p_HTriggerLight = new Paint(); // �����g���K�[����
	private final Paint p_BiasDeep = new Paint(); // �o�C�A�X�Z��
	private final Paint p_BiasLight = new Paint(); // �o�C�A�X����
	private final Paint p_VTriggerDeep = new Paint(); // �����g���K�[�Z��
	private final Paint p_VTriggerLight = new Paint(); // �����g���K�[����

	private int sampleLength;
	private double pos_bitmap_height,pos_bitmap_width,hpos_bitmap_height,hpos_bitmap_width;
	private Bitmap matrix = null;
	private Bitmap bm_pos_b = BitmapFactory.decodeResource(getResources(),R.drawable.pos_b);
	private Bitmap bm_pos_v = BitmapFactory.decodeResource(getResources(),R.drawable.pos_v);
	private Bitmap bm_pos_h = BitmapFactory.decodeResource(getResources(),R.drawable.pos_h);

	private double[] data = null;	//�g�`�f�[�^


	private double h_trigger_pos = 0;
	private double bias_pos = 0;
	private double v_trigger_pos = USBOscilloscopeHost.SAMPLE_MAX/2;

	private boolean isNewWaveSet;
	private boolean running;

	private boolean isDrawVerticalLine = false;
	private boolean isDrawHorizontalLine = false;
	private boolean isDrawBiasLine = false;



    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Paint ��`
        p_wave.setColor(Color.argb(255, 255, 0, 0));
        p_waku.setColor(Color.argb(255, 128, 128, 128));
        p_waku.setStyle(Paint.Style.STROKE); //�h��Ԃ����Ȃ�
        p_masu.setColor(Color.argb(255, 60, 60, 60));
        p_HTriggerDeep.setColor(Color.argb(255, 0, 255, 0));
        p_HTriggerLight.setColor(Color.argb(100, 0, 255, 0));
        p_BiasDeep.setColor(Color.argb(255, 0, 255, 255));
        p_BiasLight.setColor(Color.argb(100, 0, 255, 255));
        p_VTriggerDeep.setColor(Color.argb(255, 255, 0, 0));
        p_VTriggerLight.setColor(Color.argb(100, 255, 0, 0));

    }

	@Override
	public void onWindowFocusChanged (boolean hasWindowFocus){
		Log.i("++ onWindowForcusChanged ++", TAG);
		invalidate();
	}

	//
	//		������
	//
	public void init(int w,int h){	// w = set width ,h = set height
		if(matrix != null)return;	// ��x�������s

		width = (double)w;
		height = (double)h;

		// ��̑�����`
		float line_stroke_width = (float)(width/200);
		float wave_stroke_width = (float)((int)width/500);
		p_wave.setStrokeWidth(wave_stroke_width);
		if(width>800){
			p_wave.setAntiAlias(false);
		} else {
			p_wave.setAntiAlias(true);
		}

        p_HTriggerDeep.setStrokeWidth(line_stroke_width);
        p_HTriggerDeep.setStrokeCap(Paint.Cap.ROUND);
        p_HTriggerLight.setStrokeWidth(line_stroke_width);
        p_BiasDeep.setStrokeWidth(line_stroke_width);
        p_BiasLight.setStrokeWidth(line_stroke_width);
        p_VTriggerDeep.setStrokeWidth(line_stroke_width);
        p_VTriggerLight.setStrokeWidth(line_stroke_width);

        p_masu.setStrokeWidth((float)(width/600));

		//
		// �}�X�ڃr�b�g�}�b�v�����
		//

		setLayoutParams(new LinearLayout.LayoutParams(w,h));
		double widthCenter = width/2.0;
		double heightCenter = height/2.0;
		double heightDiv8 = height/8.0;
		double widthDiv10 = width/10.0;

		matrix = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(matrix);

		c.drawColor(Color.BLACK);	// �L�����o�X�����œh��Ԃ�

	    Paint p_point = new Paint();
	    p_point.setColor(Color.argb(255, 255, 255, 255));
	    p_point.setStrokeWidth((float)((int)(width / 1000)));

		float p;
		//���̃}�X�ڂ�`��
		for(int i=1;i<8;i++){
			p = (float)(i*heightDiv8);
			//c.drawLine(0, p, (float)width, p, p_masu);
			for(int j=1;j<50;j++){
				c.drawPoint((float)((width/50)*j), p, p_point);
			}
		}

		//�c�̃}�X�ڂ�`��
		for(int i=1;i<10;i++){
			p = (float)(i*widthDiv10);
			//c.drawLine(p, 0, p, (float) height, p_masu);
			for(int j=1;j<40;j++){
				c.drawPoint(p, (float)((height/40)*j), p_point);
			}
		}

		c.drawRect(0, 0, (float)(width-1), (float)(h-1), p_waku);	// �g�`��
//		c.drawLine((float) widthCenter,0,(float) widthCenter,(float)height, p_waku);		// ���S���`��
//		c.drawLine(0,(float) heightCenter,(float) width,(float) heightCenter, p_waku);

		//
		//		���ݕ`��
		//

		// �������S
		float centerLineWidth = (float)(width/100);
		float verticalLineDrawStartX = (float)(widthCenter - centerLineWidth/2);
		float verticalLineDrawEndX = (float)(verticalLineDrawStartX + centerLineWidth);
		for(int i=1;i<40;i++){
			float y = (float)((height/40)*i);
			c.drawLine(verticalLineDrawStartX,y,verticalLineDrawEndX,y,p_waku);
		}
		// �������[
		verticalLineDrawStartX = 0;
		verticalLineDrawEndX = centerLineWidth;
		for(int i=1;i<40;i++){
			float y = (float)((height/40)*i);
			if(i%5 == 0){	// division��؂�̏��͒�������
				c.drawLine(verticalLineDrawStartX,y,(verticalLineDrawEndX+(centerLineWidth/2)),y,p_waku);
			} else {
				c.drawLine(verticalLineDrawStartX,y,verticalLineDrawEndX,y,p_waku);
			}
		}
		// �����E�[
		verticalLineDrawStartX = (float)(width-centerLineWidth);
		verticalLineDrawEndX = (float)width;
		for(int i=1;i<40;i++){
			float y = (float)((height/40)*i);
			c.drawLine(verticalLineDrawStartX,y,verticalLineDrawEndX,y,p_waku);
			if(i%5 == 0){	// division��؂�̏��͒�������
				c.drawLine(verticalLineDrawStartX-(centerLineWidth/2),y,verticalLineDrawEndX,y,p_waku);
			} else {
				c.drawLine(verticalLineDrawStartX,y,verticalLineDrawEndX,y,p_waku);
			}
		}


		// �������S
		float horizontalLineDrawStartY = (float)(heightCenter - centerLineWidth/2);
		float horizontalLineDrawEndY = (float)(horizontalLineDrawStartY + centerLineWidth);
		for(int i=1;i<50;i++){
			float x = (float)((width/50)*i);
			c.drawLine(x,horizontalLineDrawStartY,x,horizontalLineDrawEndY,p_waku);
		}
		//������[
		horizontalLineDrawStartY = 0;
		horizontalLineDrawEndY = centerLineWidth;
		for(int i=1;i<50;i++){
			float x = (float)((width/50)*i);
			if(i%5 > 0){
				c.drawLine(x,horizontalLineDrawStartY,x,horizontalLineDrawEndY,p_waku);
			} else {
				c.drawLine(x,horizontalLineDrawStartY,x,horizontalLineDrawEndY+(centerLineWidth/2),p_waku);
			}
		}
		//�������[
		horizontalLineDrawStartY = (float)(height - centerLineWidth);
		horizontalLineDrawEndY = (float)height;
		for(int i=1;i<50;i++){
			float x = (float)((width/50)*i);
			if(i%5 > 0){
				c.drawLine(x,horizontalLineDrawStartY,x,horizontalLineDrawEndY,p_waku);
			} else {
				c.drawLine(x,horizontalLineDrawStartY-(centerLineWidth/2),x,horizontalLineDrawEndY,p_waku);
			}
		}


		// �|�W�V�����p�r�b�g�}�b�v�̐ݒ�
		pos_bitmap_height = heightDiv8/3;
		pos_bitmap_width = pos_bitmap_height * (46.0/64.0);
		hpos_bitmap_height = heightDiv8/3;
		hpos_bitmap_width = hpos_bitmap_height * (64.0/96.0);
		bm_pos_b = Bitmap.createScaledBitmap(bm_pos_b,(int)(pos_bitmap_width),(int)(pos_bitmap_height),false);
		bm_pos_v = Bitmap.createScaledBitmap(bm_pos_v,(int)(pos_bitmap_width),(int)(pos_bitmap_height),false);
		bm_pos_h = Bitmap.createScaledBitmap(bm_pos_h,(int)(hpos_bitmap_width),(int)(hpos_bitmap_height),false);
	}

	public void run(){
		running = true;
		isNewWaveSet = false;

		while(running){
			if(isNewWaveSet){
				invalidate();
			} else {
			    try{
			    	Thread.sleep(1L);
			    }catch(InterruptedException e){}
			}
		}

	}

	private void drawTriggerLine(Canvas c){
		if(isDrawVerticalLine){
			c.drawLine(0,(float) v_trigger_pos,(float) width,(float) v_trigger_pos,p_VTriggerLight);		// �����g���K�[���C���`��
		} else if(isDrawHorizontalLine){
			c.drawLine((float) h_trigger_pos,0,(float) h_trigger_pos,(float) height,p_HTriggerLight);		// �����g���K�[���C���`��
		} else if(isDrawBiasLine){
			c.drawLine(0,(float) bias_pos,(float) width,(float) bias_pos,p_BiasLight);						// DC�o�C�A�X���C���`��
		}

		// �g���K�[�ʒu�`��
		//c.drawLine((float)h_trigger_pos,0,(float)h_trigger_pos,(float) (height*0.04f), p_HTriggerDeep);					//�����g���K�[�}�[�J�[
		//c.drawLine(0,(float) bias_pos,(float) (height*0.04f),(float) bias_pos,p_BiasDeep);
		//c.drawLine((float) width,(float) v_trigger_pos,(float) (width-(height*0.04f)),(float) v_trigger_pos,p_VTriggerDeep);		//�����g���K�[�}�[�J�[
		//if(D)Log.d(TAG,"h_trigger_pos" + Double.toString(h_trigger_pos));
		c.drawBitmap(bm_pos_h, (float)(h_trigger_pos - (hpos_bitmap_width/2)), 0, null);
		c.drawBitmap(bm_pos_b, 0, (float) (bias_pos-(pos_bitmap_height/2)), null);
		c.drawBitmap(bm_pos_v, (float) (width-pos_bitmap_width), (float) (v_trigger_pos-(pos_bitmap_height/2)), null);

	}

	// ��ԉ���0 �A�オ1
	private void drawWaveform(Canvas c){
		// �g�`��`��

		//�G���[�`�F�b�N
		if(width <= 0 || data.length <= 0)return;

		final double[] sample = data;		// �`�撆�ɕύX����Ȃ��悤��
		isNewWaveSet = false;

		double deltaX;	// x�̑���
		double deltaI;	// i�̑���
		int numDrawLines;	// Line�`���

		if(width > sampleLength){		//  �O���t�̕� �� �T���v�����@(���h�b�g���x)
			// �`��񐔂́@�T���v����-1
			numDrawLines = sampleLength-1;
			deltaX =  width / (double) numDrawLines;
			deltaI = 1.0;

		} else {						//  �O���t�̕� �� �T���v�����@�i��h�b�g���x�j
			//�@�`��񐔂́@���s�N�Z����-1
			numDrawLines = ((int)width)-1;
			deltaX = 1.0;
			deltaI = (double) sampleLength / (double) numDrawLines;
		}

		float[] wave = new float[numDrawLines*4];
		wave[0] = 0f; 							// start x0 = 0
		wave[1] = (float)(height * (1.0 - sample[0]));	// start y0

		double fi = deltaI;		// float��index

		// ���W�Z�b�g
		final int loop_stop = wave.length-4;
		for(int j = 2;; j += 4) {
			wave[j] = (float) (wave[j-2] + deltaX);						// stop x
			wave[j+1] = (float) (height * (1.0 - sample[(int)fi]));	// stop y

			if(j > loop_stop)break;

			wave[j+2] = wave[j];								// start x
			wave[j+3] = wave[j+1];								// start y
			fi += deltaI;
		}


		c.drawLines(wave,p_wave);
	}

	public void endThread() {
		running = false;
	}


    public int frameRateGetAndReset(){
    	int tmp = FrameRate;
    	FrameRate = 0;
    	return tmp;
    }

    // ��ʉE�[��1������0���[��-1
    public void setHTriggerPos(double t){
    	if (t > 1.0){
    		t = 1.0;
    	} else if(t < -1.0){
    		t = -1.0;
    	}

   		final double width_half = width/2.0;
		h_trigger_pos = width_half + (width_half * t);

		invalidate();
    }

    // ��ԉ����O�A��ԏオ�P
    public void setVTriggerPos(double t){
    	if (t<0) {
    		t = 0;
    	} else if(t>1.0){
    		t = 1.0;
    	}

    	v_trigger_pos = height - t*height;
    	invalidate();
    }

    // ��ԉ����O�A��ԏオ�P
    public void setBiasPos(double t){
    	if (t<0) {
    		t = 0;
    	} else if(t>1.0){
    		t = 1.0;
    	}

        bias_pos = height - t*height;
        invalidate();
    }

    public void setWave(double []samples,int sampleLen){
    	if((samples != null) && (samples.length > 0)){	// �G���[�`�F�b�N
    		data = samples.clone();
    		isNewWaveSet = true;
    		sampleLength = sampleLen;

    		invalidate();
    	}
    }

    public void drawVerticalLine(){
    	isDrawVerticalLine = true;
    }

    public void drawHorizontalLine(){
    	isDrawHorizontalLine = true;
    }

    public void drawBiasLine(){
    	isDrawBiasLine = true;
    }

    public void unDrawLine(){
    	isDrawVerticalLine = false;
    	isDrawHorizontalLine = false;
    	isDrawBiasLine = false;
    }

	@Override
	protected void onMeasure(int w, int h ){
	        super.onMeasure(w,h);
	}

	public void onDraw(Canvas c){

		if(D)Log.d(TAG,"doDraw Start");
		if(c == null)return;

		//drawMatrix(c);
		if(matrix != null)c.drawBitmap(matrix, 0, 0, null);
		if(data != null && data.length>0)drawWaveform(c);
		drawTriggerLine(c);


		FrameRate++;
		//if(D)Log.d(TAG,"doDraw End");

	}


}
