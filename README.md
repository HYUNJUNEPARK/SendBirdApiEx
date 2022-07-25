해야할 것들
_파이어베이스 서버 연결
_메시지 서버에서 수신
_메시지 암호화
_복호화 키 저장(우선은 shared preference)


해결해야할 것들 
_로그인 시 프로그래스바 비동기 처리 / login activity -uikitcalucnbuttonClicked README 정리
_리사이클러뷰 비동기 처리
_basefragment -> 이걸 사용하면 binding variation 초기화가 안되서(더 서칭해봐야함) 코드가 전체적으로 지져분해짐 -> 오히려 없애는 게 나을 지도 ??

해결
_fragment binding null pointer exception
_최초 mainactivity 실행 후 ChannelListFragment 가 실행이 안되는 이슈가 있음
_channelListFragment -> 채널 생성 버튼 시 유저 없이 공백이라도 채팅방 생성 이슈




//////////





////
//******* 후반 작업할 때 Activity 를 불러오는 것 참고할 것 !
프래그먼트 화면 전환 시 상태 유지하기 (FragmentManager)
https://hanyeop.tistory.com/425

Android) Fragment 에서 View Binding 문제점, 제대로 사용하기</br>
https://yoon-dailylife.tistory.com/57</br>