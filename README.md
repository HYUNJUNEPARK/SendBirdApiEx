# Sendbird Api chat app

AOS 외부앱 실행

```kotlin
/*
아래 함수를 실행시켜 보고 원하는 데이터가 리턴되지 않는다면 Manifest 에 아래 권한 추가
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>
*/

//디바이스 내에 com.example.testapp 으로 시작하는 앱이 설치되어 있는지 확인하는 함수
private fun isUiKitAppOnMyDevice(): Boolean {
    var isExist = false
    val packageManager = packageManager
    val packages: List<PackageInfo> = packageManager.getInstalledPackages(0)
    try {
        for (info: PackageInfo in packages) {
            if (info.packageName == "com.example.testapp") {
                isExist = true
                break
            }
        }
    }
    catch (e: Exception) {
        isExist = false
    }
    return isExist
}

//앱이 설치되어 있는 경우 -> 앱 실행
val intent = packageManager.getLaunchIntentForPackage("com.example.testapp")?.run {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    action = MY_APP_INTENT_ACTION
    putExtra(INTENT_NAME_MY_DATA, "MyData")
}
startActivity(intent)

//앱이 설치되지 않은 경우 -> 앱스토어 이동
val url = "market://detail?id=" + "${TARGET_APP_PACKAGENAME}"
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
startActivity(intent)
```




//////
해야할 것들
_파이어베이스 서버 연결
_메시지 암호화
_복호화 키 저장(우선은 shared preference)


해결해야할 것들 
_로그인 시 프로그래스바 비동기 처리
_리사이클러뷰 비동기 처리
_basefragment -> 이걸 사용하면 binding variation 초기화가 안되서(더 서칭해봐야함) 코드가 전체적으로 지져분해짐 -> 오히려 없애는 게 나을 지도 ??

해결
_login activity -uikitcalucnbuttonClicked README 정리
_fragment binding null pointer exception
_최초 mainactivity 실행 후 ChannelListFragment 가 실행이 안되는 이슈가 있음
_channelListFragment -> 채널 생성 버튼 시 유저 없이 공백이라도 채팅방 생성 이슈


//////////

참고 링크

//******* 후반 작업할 때 Activity 를 불러오는 것 참고할 것 !
프래그먼트 화면 전환 시 상태 유지하기 (FragmentManager)
https://hanyeop.tistory.com/425

Android) Fragment 에서 View Binding 문제점, 제대로 사용하기</br>
https://yoon-dailylife.tistory.com/57</br>

AOS 외부 앱 실행
https://www.fun25.co.kr/blog/android-execute-3rdparty-app/?category=003

Android - PackageManager로 Package 정보 가져오기
https://codechacha.com/ko/android-query-package-info/

파이어베이스 그레들리 셋팅 방법(buildscript 설명 참고)
https://gift123.tistory.com/68



