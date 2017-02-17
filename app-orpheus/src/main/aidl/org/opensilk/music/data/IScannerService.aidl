// IScannerService.aidl
package org.opensilk.music.data;

// Declare any non-default types here with import statements
import android.os.ResultReceiver;
import android.media.browse.MediaBrowser.MediaItem;
import org.opensilk.music.data.ISubscription;

interface IScannerService {
    ISubscription scanItem(in MediaItem mediaItem, in ResultReceiver cb);
}
