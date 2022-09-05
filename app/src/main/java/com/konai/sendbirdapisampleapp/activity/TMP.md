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