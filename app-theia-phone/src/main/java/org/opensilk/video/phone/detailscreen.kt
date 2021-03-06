package org.opensilk.video.phone

import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import org.opensilk.media.*
import org.opensilk.video.*
import org.opensilk.video.phone.databinding.ActivityDetailBinding
import org.opensilk.video.phone.databinding.DetailActionsCardBinding
import org.opensilk.video.phone.databinding.DetailOverviewCardBinding
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Created by drew on 8/7/17.
 */
@Module
abstract class DetailScreenModule {
    @ContributesAndroidInjector
    abstract fun injector(): DetailActivity
}

class DetailActivity: BaseVideoActivity() {

    lateinit var mBinding: ActivityDetailBinding
    lateinit var mViewModel: DetailViewModel

    @Inject lateinit var mAdapter: DetailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail)
        mBinding.recycler.layoutManager = LinearLayoutManager(this)
        mBinding.recycler.adapter = mAdapter

        mViewModel = fetchViewModel(DetailViewModel::class)
        mViewModel.posterUri.observe(this, LiveDataObserver {
            mAdapter.posterUri = it
        })
        mViewModel.resumeInfo.observe(this, LiveDataObserver {
            mAdapter.resumeInfo = it
        })
        mViewModel.fileInfo.observe(this, LiveDataObserver {
            mAdapter.fileInfo = it
        })
        mViewModel.videoDescription.observe(this, LiveDataObserver {
            mAdapter.videoDesc = it
        })
        mViewModel.lookupError.observe(this, LiveDataObserver {
            Snackbar.make(mBinding.coordinator, it, Snackbar.LENGTH_LONG).show()
        })

        mAdapter.mediaId = intent.getMediaIdExtra()
        mViewModel.setMediaId(intent.getMediaIdExtra())
    }
}

interface DetailActionHandler {
    fun onAction(action: DetailAction)
}

enum class DetailAction {
    PLAY,
    START_OVER,
    RESUME,
    GET_DESC,
    REMOVE_DESC
}

class DetailAdapter
@Inject constructor(): RecyclerView.Adapter<BoundViewHolder>() {

    lateinit var mediaId: MediaId

    var posterUri: Uri by Delegates.observable(Uri.EMPTY, { _, _, _ ->
        notifyItemChanged(0)
    })
    var resumeInfo: VideoResumeInfo by Delegates.observable(VideoResumeInfo(), { _, _, _ ->
        notifyItemChanged(0)
    })
    var videoDesc: VideoDescInfo by Delegates.observable(VideoDescInfo(), { _, _, _ ->
        notifyItemChanged(1)
    })
    var fileInfo: VideoFileInfo by Delegates.observable(VideoFileInfo(), { _, _, _ ->
        notifyItemChanged(2)
    })


    override fun onBindViewHolder(holder: BoundViewHolder, position: Int) {
        when (holder) {
            is DetailPlayActionsViewHolder -> {
                holder.bind(posterUri, resumeInfo, mediaId)
            }
            is DetailOverviewViewHolder -> {
                holder.bind(videoDesc)
            }
        }
    }

    override fun getItemCount(): Int = 2//3

    override fun getItemViewType(position: Int): Int = when (position) {
        0 -> R.layout.detail_actions_card
        1 -> R.layout.detail_overview_card
        else -> TODO()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoundViewHolder = when (viewType) {
        R.layout.detail_actions_card -> {
            DetailPlayActionsViewHolder(DataBindingUtil.inflate(LayoutInflater.from(
                    parent.context), viewType, parent, false))
        }
        R.layout.detail_overview_card -> {
            DetailOverviewViewHolder(DataBindingUtil.inflate(LayoutInflater.from(
                    parent.context), viewType, parent, false))
        }
        else -> TODO()
    }

}

class DetailPlayActionsViewHolder(val binding: DetailActionsCardBinding):
        BoundViewHolder(binding.root), DetailActionHandler {

    lateinit var mediaId: MediaId

    fun bind(posterUri: Uri, resumeInfo: VideoResumeInfo, mediaId: MediaId) {
        this.mediaId = mediaId
        binding.actionHandler = this
        val actions = HashSet<DetailAction>()
        if (resumeInfo.lastCompletion in 1..979) {
            actions.add(DetailAction.RESUME)
            actions.add(DetailAction.START_OVER)
            binding.resumeTime = humanReadableDuration(resumeInfo.lastPosition)
        } else {
            actions.add(DetailAction.PLAY)
        }
        binding.actions = actions
        if (!posterUri.isEmpty()) {
            Glide.with(binding.root.context)
                    .asDrawable()
                    .apply(RequestOptions().centerCrop())
                    .load(posterUri)
                    .into(binding.poster)
        } else {
            binding.poster.setImageResource(R.drawable.ic_movie_48dp)
        }
    }

    override fun unbind() {
        Glide.with(binding.root.context).clear(binding.poster)
    }

    override fun onAction(action: DetailAction) {
        when (action) {
            DetailAction.RESUME -> {
                val intent = Intent(binding.root.context, PlaybackActivity::class.java)
                        .setAction(ACTION_RESUME)
                        .putMediaIdExtra(mediaId)
                binding.root.context.startActivity(intent)
            }
            DetailAction.PLAY,
            DetailAction.START_OVER -> {
                val intent = Intent(binding.root.context, PlaybackActivity::class.java)
                        .setAction(ACTION_PLAY)
                        .putMediaIdExtra(mediaId)
                binding.root.context.startActivity(intent)
            }
            else -> TODO()
        }
    }
}

class DetailOverviewViewHolder(val binding: DetailOverviewCardBinding):
        BoundViewHolder(binding.root), DetailActionHandler {

    fun bind(descInfo: VideoDescInfo) {
        binding.actionHandler = this
        binding.overviewText = descInfo.overview
        binding.hasDescription = !descInfo.overview.isNullOrBlank()
    }

    override fun unbind() {

    }

    override fun onAction(action: DetailAction) {
        when (action) {
            DetailAction.GET_DESC -> {

            }
            else -> TODO()
        }
    }
}

