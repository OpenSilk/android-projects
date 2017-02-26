package org.opensilk.music.ui.fragments

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.opensilk.music.R
import org.opensilk.music.databinding.SheetPlayingBinding

/**
 * Created by drew on 8/15/16.
 */
class PlayingSheetFragment: Fragment() {

    private var mBinding: SheetPlayingBinding? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.sheet_playing, container, false)
        return mBinding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mBinding = null
    }

}