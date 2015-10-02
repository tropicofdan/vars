package vars.annotation.ui.videofile.quicktime;

import org.mbari.util.Tuple2;
import vars.ToolBelt;
import vars.annotation.AnnotationDAOFactory;
import vars.annotation.VideoArchive;
import vars.avplayer.AbstractAccessUI;
import vars.avplayer.DefaultVideoPlayerDialogUI;
import vars.avplayer.LookupService;
import vars.avplayer.VideoParams;
import vars.avplayer.VideoPlayerController;
import vars.avplayer.VideoPlayerDialogUI;

import javax.inject.Inject;
import java.awt.*;

/**
 * Created by brian on 1/7/14.
 * @deprecated
 */
public class QTAccessUI extends AbstractAccessUI {

    private VideoPlayerDialogUI dialog;
    private Window currentParent;
    //private QTController controller = new QTController();
    private final LookupService lookupService;

    @Inject
    public QTAccessUI(AnnotationDAOFactory daoFactory, LookupService lookupService) {
        super(daoFactory);
        this.lookupService = lookupService;
    }

    @Override
    public VideoPlayerDialogUI getOpenDialog(Window parent, ToolBelt toolBelt) {

        // dispose of old dialog if the parent window reference changes
        if (dialog != null && parent != currentParent) {
            //dialog.dispose();
            dialog = null;
        }

        // create new dialog if needed
        if (dialog == null) {
            dialog = new DefaultVideoPlayerDialogUI(parent, toolBelt, this, lookupService);
            currentParent = parent;
        }

        return dialog;
    }

    @Override
    public Tuple2<VideoArchive, VideoPlayerController> openMoviePlayer(VideoParams videoParams) {
//        Optional<VideoArchive> videoArchiveOpt = findByLocation(videoParams.getMovieLocation());
//        VideoArchive videoArchive = videoArchiveOpt.orElseGet(() -> createVideoArchive(videoParams));
//        controller.getVideoControlService().connect(videoParams.getMovieLocation(),
//                videoParams.getTimeSourceOpt().orElse(TimeSource.AUTO));
//        return new Tuple2<>(videoArchive, controller);
        throw new UnsupportedOperationException("VARS is dropping direct QuickTime support");
    }
}
