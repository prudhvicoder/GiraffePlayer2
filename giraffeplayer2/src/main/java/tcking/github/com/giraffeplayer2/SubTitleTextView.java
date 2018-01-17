package tcking.github.com.giraffeplayer2;

import android.content.Context;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.util.AttributeSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by codegama on 6/11/17.
 */

public class SubTitleTextView extends AppCompatTextView implements Runnable {

    private static final String TAG = "SubtitleView";
    private static final boolean DEBUG = false;
    private static final int UPDATE_INTERVAL = 300;
    private GiraffePlayer player;
    private TreeMap<Long, Line> track;

    public SubTitleTextView(Context context) {
        super(context);
    }

    public SubTitleTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SubTitleTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    subTitleTextView.setPlayer(player);
//                        subTitleTextView.setSubSource(R.raw.kind, MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP);
    @Override
    public void run() {
        if (player != null && track != null) {
            int seconds = player.getCurrentPosition() / 1000;
            setText((DEBUG?"[" + secondsToDuration(seconds) + "]" : "") + Html.fromHtml(getTimedText(player.getCurrentPosition())));
        }
        postDelayed(this, UPDATE_INTERVAL);
    }

    private String getTimedText(long currentPosition) {
        String result = "";
        for(Map.Entry<Long, Line> entry: track.entrySet()){
            if (currentPosition < entry.getKey()) break;
            if (currentPosition < entry.getValue().to) result = entry.getValue().text;
        }
        return result;
    }

    public String secondsToDuration(int seconds) {
        return String.format("%02d:%02d:%02d", seconds / 3600,
                (seconds % 3600) / 60, (seconds % 60), Locale.US);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        postDelayed(this, UPDATE_INTERVAL);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(this);
    }

    public void setPlayer(GiraffePlayer player) {
        this.player = player;
    }

    public void setSubSource(String url, String mime){
        if (mime.equals(MediaPlayer.MEDIA_MIMETYPE_TEXT_SUBRIP)) {
            track = getSubtitleFile(url);
        }
        else throw new UnsupportedOperationException("Parser only built for SRT subs");
    }


    public static TreeMap<Long, Line> parse(InputStream is) throws IOException {
        LineNumberReader r = new LineNumberReader(new InputStreamReader(is, "UTF-8"));
        TreeMap<Long, Line> track = new TreeMap<>();
        while ((r.readLine()) != null) /*Read cue number*/{
            String timeString = r.readLine();
            String lineString = "";
            String s;
            while (!((s = r.readLine()) == null || s.trim().equals(""))) {
                lineString += s + "\n";
            }
            long startTime = parse(timeString.split("-->")[0]);
            long endTime = parse(timeString.split("-->")[1]);
            track.put(startTime, new Line(startTime, endTime, lineString));
        }
        return track;
    }

    private static long parse(String in) {
        long hours = Long.parseLong(in.split(":")[0].trim());
        long minutes = Long.parseLong(in.split(":")[1].trim());
        long seconds = Long.parseLong(in.split(":")[2].split(",")[0].trim());
        long millies = Long.parseLong(in.split(":")[2].split(",")[1].trim());

        return hours * 60 * 60 * 1000 + minutes * 60 * 1000 + seconds * 1000 + millies;

    }

    private TreeMap<Long, Line> getSubtitleFile(int resId) {
        InputStream inputStream = null;
        try {
            inputStream = getResources().openRawResource(resId);
            return parse(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private TreeMap<Long, Line> getSubtitleFile(String url) {
        InputStream inputStream = null;
        try {
            inputStream = new URL(url).openStream();
            return parse(inputStream);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    public static class Line {
        long from;
        long to;
        String text;


        public Line(long from, long to, String text) {
            this.from = from;
            this.to = to;
            this.text = text;
        }
    }
}
