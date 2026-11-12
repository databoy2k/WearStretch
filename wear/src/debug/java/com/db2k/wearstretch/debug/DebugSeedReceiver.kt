package com.db2k.wearstretch.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.db2k.wearstretch.model.DataLayerPaths
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.io.File

/**
 * DEBUG BUILDS ONLY. Lets an unpaired emulator get routines without a phone:
 *   adb push routines.json /data/local/tmp/ ; run-as copy to files/seed_routines.json
 *   adb shell am broadcast -n com.db2k.wearstretch/.debug.DebugSeedReceiver
 * Writes the same DataItem the phone would, so the normal sync path runs.
 */
class DebugSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val file = File(context.filesDir, "seed_routines.json")
        if (!file.exists()) { Log.w("WearStretchDebug", "no ${file.path}"); return }
        val req = PutDataMapRequest.create(DataLayerPaths.ROUTINE_LIST)
        req.dataMap.putString("routines_json", file.readText())
        req.dataMap.putLong("timestamp", System.currentTimeMillis())
        Wearable.getDataClient(context).putDataItem(req.asPutDataRequest().setUrgent())
            .addOnSuccessListener { Log.i("WearStretchDebug", "seeded routines") }
            .addOnFailureListener { Log.e("WearStretchDebug", "seed failed", it) }
    }
}
