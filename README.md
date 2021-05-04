
![image](https://user-images.githubusercontent.com/47134564/115210597-b6fa5900-a139-11eb-9722-ed0573457f71.png)


노드 서버 정보는 다음 repo의 Nodeserver폴더에 있다.

https://github.com/chosaihim/socketIOTest

# 1.04, 4 version picpho

  

### 💁🏻‍♂️ 요약

### 현재 사용자수 23명
### 버전 검토일 21년 05월 04일

  
## 변경내용

### 1 . IP 변경

  

고정아이피 설정으로 IP 변경.

  

### 2 . 인앱 업데이트 추가

  

LoginActivity에 인앱 업데이트를 추가하였다.

  

인앱 업데이트는 플레이스토어에 들어가지 않아도 update를 진행하도록 해주는 google에서 만든 방법이다. IMMEDIATE속성을 넣어 바로 업데이트를 하도록 만들었음.

  

### 3 . 처음 픽포 받았을 때 친구목록 두배되는 현상

1.03버전에서 socket on이 두번이 된다고 생각해서 destroy에 socket off를 넣어봤는데 해결이 되지 않음. 잘생각해보니 socket emit이 resume에 있기 때문에 사용자가 인앱 브라우저에 갔다가 오면 emit이 한번 더 되는 것 같다고 생각함.

  

####  해결방법 → 그래서 친구목록 list가 텅 비어있을 때에만 on function이 일어나도록 바꿈.
