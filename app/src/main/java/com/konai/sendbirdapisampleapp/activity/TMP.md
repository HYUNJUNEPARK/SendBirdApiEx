//    //TODO 닉네임 등록/변경
//    private fun enrollNickName() {
//        val params = UserUpdateParams().apply {
//            nickname = USER_NICKNAME
//        }
//        SendbirdChat.updateCurrentUserInfo(params) { exception ->
//            if (exception != null) {
//                binding.progressBarLayout.visibility = View.GONE
//                Log.e(TAG, "update Current UserInfo Error : $exception")
//                showToast("유저 닉네임 업데이트 에러 : $exception")
//                return@updateCurrentUserInfo
//            }
//        }
//    }


    private suspend fun confirmSharedSecretKey_TEST() = withContext(Dispatchers.IO) {
        var publicKey: PublicKey ?= null
        var metadata: String ?= null

        //publicKey
        launch {
            localDB.keyIdDao().getKeyId(channelURL).let { keyId ->
                remoteDB!!.collection(FIRESTORE_DOCUMENT_PUBLIC_KEY)
                    .get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            if (document.data[FIRESTORE_FIELD_USER_ID] == friendId) {
                                try {
                                    publicKey = ECKeyUtil.coordinatePublicKey(
                                        affineX = document.data[FIRESTORE_FIELD_AFFINE_X].toString(),
                                        affineY = document.data[FIRESTORE_FIELD_AFFINE_Y].toString()
                                    )
                                    Log.d(TAG, "1. publicKey coordinated")
                                }
                                catch (e: Exception) {
                                    //TODO PublicKey 생성에 실패한 상황
                                    e.printStackTrace()
                                    return@addOnSuccessListener
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        return@addOnFailureListener
                    }
            }
        }.join()

        //metadata
        launch {
            val data = listOf(CHANNEL_META_DATA)
            GroupChannel.getChannel(channelURL) { channel, e1 ->
                if (e1 != null) {
                    e1.printStackTrace()
                    return@getChannel
                }
                channel!!.getMetaData(data) { metaDataMap, e2 ->
                    if (e2 != null) {
                        e2.printStackTrace()
                        return@getMetaData
                    }
                    try {
                        metadata = metaDataMap!!.values.toString()
                            .substring(1 until metaDataMap.values.toString().length)
                        Log.d(TAG, "get metadata : $metadata")
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                        return@getMetaData
                    }
                }
            }
        }.join()

        if (publicKey == null || metadata == null) return@withContext

        //ssk
        strongBox.generateSharedSecretKey(
            USER_ID,
            publicKey!!,
            metadata!!
        ).let { sharedSecretKey ->
            //2.4 ESP 에 sharedSecretKey 등록
            /*
            EncryptedSharedPreferences
            =======================================
            KeyId(Secure Random) | SharedSecretKey |
            =======================================
            */
            espm.putString(
                metadata!!,
                sharedSecretKey
            )
        }

        //로컬 DB 저장
        localDB.keyIdDao().insert(
            KeyIdEntity(
                channelURL,
                metadata!!
            )
        )
    }
