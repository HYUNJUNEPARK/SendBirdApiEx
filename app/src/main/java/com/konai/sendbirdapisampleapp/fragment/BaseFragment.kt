package com.konai.sendbirdapisampleapp.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

abstract class BaseFragment<T: ViewDataBinding>(@LayoutRes private val layoutId: Int): Fragment() {
    private var _binding: T? = null
    val binding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        //TODO binding.var = this
        //https://stackoverflow.com/questions/67944233/androidonclick-attribute-is-not-working-through-data-binding?noredirect=1&lq=1
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    //TODO CHECK THIS BLOG TO SOLVE ISSUES
    //https://leveloper.tistory.com/210
    //https://yoon-dailylife.tistory.com/57
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onDestroyView() {
        super.onDestroyView()

    }

    protected open fun initView() { }
}