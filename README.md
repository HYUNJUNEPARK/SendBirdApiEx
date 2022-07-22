해야할 것들
메시지 서버에서 수신
메시지 암호화
복호화 키 저장(우선은 shared preference)


해결해야할 것들 
3. 최초 mainactivity 실행 후 ChannelListFragment 가 실행이 안되는 이슈가 있음
4. 리사이클러뷰 비동기 처리
5. 로그인 시 프로그래스바 비동기 처리 / 로그인 edit text view 가 공백이면 로그인 버튼 비활성화 / login activity -uikitcalucnbuttonClicked README 정리
6. basefragment -> 이걸 사용하면 binding variation 초기화가 안되서(더 서칭해봐야함) 코드가 전체적으로 지져분해짐 -> 오히려 없애는 게 나을 지도 ??

해결
7. channelListFragment -> 채널 생성 버튼 시 유저 없이 공백이라도 채팅방 생성 이슈
