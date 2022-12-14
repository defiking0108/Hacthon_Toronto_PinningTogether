package org.topnetwork.pintogether.ui.fragment.view

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps.*
import com.amap.api.maps.AMap.OnMapTouchListener
import com.amap.api.maps.LocationSource.OnLocationChangedListener
import org.topnetwork.pintogether.R
import org.topnetwork.pintogether.adapter.PinNftListAdapter
import org.topnetwork.pintogether.base.app.BaseFragment
import org.topnetwork.pintogether.databinding.FragmentPinBinding
import org.topnetwork.pintogether.map.Location
import org.topnetwork.pintogether.model.NearBy
import org.topnetwork.pintogether.startToReceiveActivity
import org.topnetwork.pintogether.ui.fragment.vm.PinFragmentVm
import java.lang.Exception
import android.graphics.Bitmap
import android.graphics.Point

import com.bumptech.glide.request.target.SimpleTarget

import com.bumptech.glide.Glide

import android.view.LayoutInflater
import android.widget.ImageView
import com.bumptech.glide.request.transition.Transition
import android.view.animation.LinearInterpolator
import androidx.lifecycle.Observer
import com.amap.api.maps.model.*
import com.amap.api.maps.model.animation.Animation
import com.amap.api.maps.model.animation.ScaleAnimation
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeResult
import com.topnetwork.zxing.IScanResultCallBack
import com.topnetwork.zxingV2.ScanManager
import com.topnetwork.zxingV2.startCaptureActivity
import org.greenrobot.eventbus.EventBus
import org.topnetwork.pintogether.base.app.BaseLazyLoadFragment
import org.topnetwork.pintogether.event.LocationChangedEvent
import org.topnetwork.pintogether.permission.PermissionChecker
import org.topnetwork.pintogether.startNftDetails
import org.topnetwork.pintogether.utils.ActivityUtils
import org.topnetwork.pintogether.utils.LogUtils
import org.topnetwork.pintogether.utils.StringUtils
import org.topnetwork.pintogether.utils.ToastUtils
import org.topnetwork.pintogether.utils.image.transform.GlideCircleTransform
import kotlin.math.roundToInt

class PinFragment : BaseLazyLoadFragment<FragmentPinBinding, PinFragmentVm>(), LocationSource,
    AMapLocationListener, OnMapTouchListener, AMap.OnMarkerClickListener,
    GeocodeSearch.OnGeocodeSearchListener {

    val scanKey = PinFragment::class.java.simpleName
    private var aMap: AMap? = null
    private var mListener: OnLocationChangedListener? = null
    private var mlocationClient: AMapLocationClient? = null
    private var mLocationOption: AMapLocationClientOption? = null

    var useMoveToLocationWithMapMode = true

    //???????????????????????????Marker
    var firstLocation = false

    //??????????????????????????????
    var projection: Projection? = null

    var mAdapter: PinNftListAdapter?=null

    override fun getLayoutResId(): Int = R.layout.fragment_pin

    override fun createViewModel(): PinFragmentVm =  ViewModelProvider(this).get(
        PinFragmentVm::class.java
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)
        cvb?.map?.onCreate(savedInstanceState)
        init()

        initRv()
        setData()

        cvb?.swip?.setOnRefreshListener {
            latlon?.run {
                viewModel?.requestNearBy(page,latitude,longitude)
            }
            cvb?.swip?.isRefreshing = false
        }

        cvb?.ivCode?.setOnClickListener {
            if (PermissionChecker.hasPermissions(context, *PermissionChecker.CAMERA_PERMISSIONS)) {
                startCaptureActivity(scanKey,100)
            } else {
                PermissionChecker.requestPermissions(
                    context as Activity,
                    PermissionChecker.REQUEST_CODE_PERMISSION_CAMERA,
                    *PermissionChecker.CAMERA_PERMISSIONS
                )
            }
        }

        ScanManager.registerScanResultCallBack(scanKey, object : IScanResultCallBack {
            override fun result(result: String?) {
                result?.run {
                    if (result.isNotEmpty()) {
                        showLoading("")
                        viewModel?.validationSignIn(result)
                    } else {
                        ToastUtils.showLong("????????????")
                    }
                    ActivityUtils.pop()
                }
            }

        })

        viewModel?.validation?.observe(viewLifecycleOwner, Observer {
            dismissLoading()
            if(!StringUtils.isEmpty(it)){
                //??????
                startToReceiveActivity(it)
            }else{
                ToastUtils.showLong("????????????")
            }
        })

        // ???????????????????????????????????????????????????
        aMap!!.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
            override fun onCameraChange(position: CameraPosition) {}
            override fun onCameraChangeFinish(postion: CameraPosition) {
                //??????????????????
                val proj = aMap!!.projection
                cvb?.ivLocation?.run {
                    var width: Int = x.roundToInt()
                    var height : Int = y.roundToInt()
                    LogUtils.eTag("SignInLocationActivity",width)
                    LogUtils.eTag("SignInLocationActivity",height)
                    val latLng1: LatLng = proj.fromScreenLocation(
                        Point( width,
                        height)
                    )
                    this@PinFragment.latlon = latLng1
                    viewModel?.requestNearBy(page,latLng1.latitude,latLng1.longitude)
                }


            }
        })
    }


    override fun onFragmentVisible() {
        super.onFragmentVisible()
        latlon?.run {
            viewModel?.requestNearBy(page,latitude,longitude)
        }
    }
    /**
     * ??????????????????
     */
    override fun onResume() {
        super.onResume()
        cvb?.map?.onResume()
        useMoveToLocationWithMapMode = true
    }

    /**
     * ??????????????????
     */
    override fun onPause() {
        super.onPause()
        cvb?.map?.onPause()
        useMoveToLocationWithMapMode = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        cvb?.map?.onSaveInstanceState(outState)
    }

    /**
     * ??????????????????
     */
    override fun onDestroy() {
        super.onDestroy()
        cvb?.map?.onDestroy()
        mlocationClient?.onDestroy()
    }


    private val BACK_LOCATION_PERMISSION = "android.permission.ACCESS_BACKGROUND_LOCATION"

    override fun afterViewCreated() {
        if (PermissionChecker.hasPermissions(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                BACK_LOCATION_PERMISSION
            )
        ) {

        } else{
            PermissionChecker.requestPermissions(
                this,
                PermissionChecker.REQUEST_CODE_PERMISSION_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                BACK_LOCATION_PERMISSION
            )
        }
    }

    override fun activate(listener: OnLocationChangedListener?) {
        mListener = listener
        if (mlocationClient == null) {
            try {
                mlocationClient = AMapLocationClient(this.context)
                mLocationOption = AMapLocationClientOption()
                //??????????????????
                mlocationClient?.setLocationListener(this)
                //??????????????????????????????
                mLocationOption?.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy)
                //??????????????????
                mLocationOption?.setInterval(2000)
                //??????????????????
                mlocationClient?.setLocationOption(mLocationOption)
                // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
                // ??????????????????????????????????????????????????????????????????2000ms?????????????????????????????????stopLocation()???????????????????????????
                // ???????????????????????????????????????????????????onDestroy()??????
                // ?????????????????????????????????????????????????????????????????????stopLocation()???????????????????????????sdk???????????????
                mlocationClient?.startLocation()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun deactivate() {
        mListener = null
        if (mlocationClient != null) {
            mlocationClient!!.stopLocation()
            mlocationClient!!.onDestroy()
        }
        mlocationClient = null
    }

    override fun onLocationChanged(amapLocation: AMapLocation?) {
        Log.e("Amap", "onLocationChanged")
        if (mListener != null && amapLocation != null) {
            if (amapLocation != null
                && amapLocation.getErrorCode() == 0
            ) {
                val latLng = LatLng(amapLocation.getLatitude(), amapLocation.getLongitude())
                Location.currentLatlng = latLng
                EventBus.getDefault().post(LocationChangedEvent(latLng))
                Location.currentAddress = amapLocation.address
                //??????????????????????????????
                if (!firstLocation && PermissionChecker.hasPermissions(
                        context,  Manifest.permission.ACCESS_COARSE_LOCATION
                    )) {
                    //????????????
                    firstLocation = true
                    //????????????,????????????????????????????????????????????????15???
                    viewModel.requestNearBy(1,Location.currentLatlng!!.latitude,Location.currentLatlng!!.longitude)
                    aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                } else {
//                    if (useMoveToLocationWithMapMode) {
//                        //???????????????????????????sdk??????????????????????????????????????????????????????????????????????????????????????????????????????
//                        startMoveLocationAndMap(latLng)
//                    } else {
//                        startChangeLocation(latLng)
//                    }
                }
            } else {
                val errText =
                    "????????????," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo()
                Log.e("AmapErr", errText)
            }
        }
    }

    override fun onTouch(p0: MotionEvent?) {
        useMoveToLocationWithMapMode = false
    }
    private var page = 1
    private var latlon:LatLng ?= null
    /**
     * ?????????
     */
    fun init() {
        if (aMap == null) {
            aMap = cvb?.map?.getMap()
            setUpMap()
        }
        cvb?.ivCurrentLocation?.setOnClickListener {
            Location.currentLatlng?.run {
                latlon = this
                aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(this, 15f))
                viewModel?.requestNearBy(page,latitude,longitude)
            }
        }
    }

    /**
     * ????????????amap?????????
     */
    private fun setUpMap() {
        aMap?.setLocationSource(this) // ??????????????????
        aMap?.setOnMarkerClickListener(this)
        aMap?.uiSettings?.setZoomControlsEnabled(false)
        aMap?.uiSettings?.isMyLocationButtonEnabled = false // ????????????????????????????????????
        aMap?.isMyLocationEnabled = true // ?????????true??????????????????????????????????????????false??????????????????????????????????????????????????????false
        //aMap?.setOnMapTouchListener(this)
    }

    private fun initRv() {
        mAdapter = PinNftListAdapter().apply {
            setOnLoadMoreListener({
                //viewModel?.loadMore()
                mAdapter?.loadMoreEnd(false)
            }, cvb?.rvList)

            setOnItemClickListener { adapter, view, position ->
                startToReceiveActivity(adapter.getItem(position) as NearBy)
            }
        }

        cvb?.rvList?.run {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

    }

    private fun setData(){
        viewModel?.nearByData?.observe(viewLifecycleOwner, {
            mAdapter?.setNewData(it)
            addMarkets(it as ArrayList<NearBy>)
        })
    }

    /**
     * ???????????????????????????????????????
     * @param latLng
     */
//    private fun startChangeLocation(latLng: LatLng) {
//        if (locationMarker != null) {
//            val curLatlng: LatLng? = locationMarker?.getPosition()
//            if (curLatlng == null || curLatlng != latLng) {
//                locationMarker?.setPosition(latLng)
//            }
//        }
//    }

    /**
     * ??????????????????????????????????????????????????????
     * @param latLng
     */
//    private fun startMoveLocationAndMap(latLng: LatLng) {
//
//        //??????????????????????????????
//        if (projection == null) {
//            projection = aMap!!.projection
//        }
//        if (locationMarker != null && projection != null) {
//            val markerLocation: LatLng? = locationMarker?.getPosition()
//            val screenPosition = aMap!!.projection.toScreenLocation(markerLocation)
//            locationMarker?.setPositionByPixels(screenPosition.x, screenPosition.y)
//        }
//
//        //??????????????????????????????????????????????????????????????????
//        myCancelCallback.setTargetLatlng(latLng)
//        //???????????????????????????????????????????????????????????????????????????2000ms ??????????????????????????????2000ms???????????????1000ms
//        //???????????????????????????myCancelCallback?????????????????????????????????
//        aMap!!.animateCamera(CameraUpdateFactory.changeLatLng(latLng), 1000, myCancelCallback)
//    }


//    var myCancelCallback: MyCancelCallback =
//        MyCancelCallback(locationMarker)

//    /**
//     * ?????????????????????????????????????????????????????????????????????????????????????????????
//     */
//    class MyCancelCallback : CancelableCallback {
//        var targetLatlng: LatLng ? = null
//        var locationMarker: Marker? = null
//        constructor( locationMarker: Marker?){
//            this.locationMarker = locationMarker
//        }
//        @JvmName("setTargetLatlng1")
//        fun setTargetLatlng(latlng: LatLng?) {
//            targetLatlng = latlng
//        }
//
//        override fun onFinish() {
//            if (locationMarker != null && targetLatlng != null) {
//                locationMarker?.setPosition(targetLatlng)
//            }
//        }
//
//        override fun onCancel() {
//            if (locationMarker != null && targetLatlng != null) {
//                locationMarker?.setPosition(targetLatlng)
//            }
//        }
//    }

    private fun addMarkets(array: ArrayList<NearBy>){
        aMap?.clear()
        aMap?.run {
            for (index in 0 until array.size){
                //????????????BitmapDescriptor??????
                val pic = arrayOf<BitmapDescriptor?>(null)
                returnPictureView(array[index].cid,object :ResultListener{
                    override fun onReturnResult(view: View?) {
                        view?.run {
                            pic[0] = BitmapDescriptorFactory.fromView(view);
                            putDataToMarkerOptions(pic[0],array[index])
                        }

                    }

                })

            }

        }

    }

    //????????????????????????
    private fun putDataToMarkerOptions(pic: BitmapDescriptor?, nearByFriend: NearBy) {
        var latLon = LatLng(nearByFriend.lat, nearByFriend.lng)
        var markerOption: MarkerOptions =
            MarkerOptions().icon(pic)
                .snippet(nearByFriend.giftId)
                .position(latLon)
                .draggable(true)
        var marker: Marker = aMap!!.addMarker(markerOption)

        val animation: Animation = ScaleAnimation(0F, 1F, 0f, 1f)
        animation.setInterpolator(LinearInterpolator())
        //??????????????????????????????
        animation.setDuration(500)
        //????????????
        marker.setAnimation(animation)
        //????????????
        marker.startAnimation()
    }

    //???????????????????????????
    private fun returnPictureView(
        imagUrl: String,
        resultListener: ResultListener
    ) {
        val markerView: View = LayoutInflater.from(context).inflate(R.layout.avator_view, null)
        val backGround: ImageView =
            markerView.findViewById<View>(R.id.iv_pic) as ImageView
        Glide.with(this)
            .asBitmap()
            .load(imagUrl)
            .transform(GlideCircleTransform())
            .error(R.drawable.ic_holder2)
            .into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap?>?
                ) {
                    backGround.setImageBitmap(resource)
                    resultListener.onReturnResult(markerView)
                }
            })
    }

    //????????????
    private interface ResultListener {
        fun onReturnResult(view: View?)
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        marker?.run {
            LogUtils.eTag("PinFragment", "????????? $snippet")
            //??????snippet????????????
            mAdapter?.run {
                var array = data
                var clickNearBy:NearBy ?= null
                for (index in 0 until array.size){
                    if(array[index].giftId.equals(snippet)){
                        clickNearBy = array[index]
                        break
                    }
                }
                clickNearBy?.run {
                    LogUtils.eTag("PinFragment", "startToReceiveActivity")
                    startToReceiveActivity(this)
                    return  true
                }

            }
        }
        return  true
    }

    override fun onRegeocodeSearched(p0: RegeocodeResult?, p1: Int) {
        TODO("Not yet implemented")
    }

    override fun onGeocodeSearched(p0: GeocodeResult?, p1: Int) {
        TODO("Not yet implemented")
    }

}