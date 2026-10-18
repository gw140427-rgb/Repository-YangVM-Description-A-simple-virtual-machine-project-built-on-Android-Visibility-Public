# QEMU 명령어 목록 (Termux / YangVM)

작성일: 2026-10-18

> 중요: 아래는 QEMU 명령어 사용 예시입니다. 실제 실행 가능 여부는 Termux에 설치된 QEMU 빌드와 지원 대상에 따라 달라집니다. Galaxy A16에서 실행·성능 테스트를 완료한 목록은 아닙니다. ISO 파일 경로와 디스크 파일 경로는 본인 환경에 맞게 바꾸세요.

공식 문서:
- QEMU 시스템 에뮬레이터 개요: https://www.qemu.org/docs/master/system/introduction.html
- 명령줄 옵션: https://www.qemu.org/docs/master/system/invocation.html
- 디스크 이미지: https://www.qemu.org/docs/master/system/images.html
- 지원 시스템 대상: https://www.qemu.org/docs/master/system/targets.html
- Termux 패키지 저장소: https://github.com/termux/termux-packages

## 1. 설치 상태 확인

```sh
command -v qemu-system-x86_64
command -v qemu-system-aarch64
command -v qemu-img
```

명령어가 출력되지 않으면 해당 실행 파일이 PATH에 없거나 설치되지 않은 것입니다. 패키지 이름과 제공 대상은 현재 Termux 저장소에서 확인하세요.

## 2. QEMU 도움말 확인

```sh
qemu-system-x86_64 --help
qemu-system-aarch64 --help
qemu-img --help
```

현재 설치된 빌드에서 지원하는 옵션을 확인할 때 사용합니다.

## 3. 가상 디스크 만들기

QCOW2 형식의 8 GiB 가상 디스크 예시:

```sh
qemu-img create -f qcow2 debian.qcow2 8G
```

디스크 이미지 정보 확인:

```sh
qemu-img info debian.qcow2
```

QCOW2의 8G는 가상 디스크의 최대 용량이며, 즉시 8 GiB를 모두 차지한다는 뜻은 아닙니다.

## 4. x86_64 게스트 ISO 부팅 예시

```sh
qemu-system-x86_64 \
  -m 1024 \
  -cdrom ./installer.iso \
  -boot order=d
```

- `-m 1024`: 게스트 메모리 1024 MiB 예시
- `-cdrom`: 부팅할 ISO 파일
- `-boot order=d`: CD-ROM 우선 부팅

ISO가 실제로 부팅 가능한지, 그래픽 표시 방식이 현재 빌드에서 지원되는지는 별도 확인이 필요합니다.

## 5. 가상 디스크로 부팅

```sh
qemu-system-x86_64 \
  -m 1024 \
  -drive file=debian.qcow2,format=qcow2
```

## 6. ISO 설치 프로그램을 가상 디스크와 함께 실행

```sh
qemu-system-x86_64 \
  -m 1024 \
  -cdrom ./installer.iso \
  -drive file=debian.qcow2,format=qcow2 \
  -boot order=d
```

설치 프로그램에서 대상 디스크를 선택할 때는 QEMU 가상 디스크를 선택해야 합니다. 실제 Android 저장소나 개인 파일을 설치 대상으로 선택하지 마세요.

## 7. ARM64 게스트 예시

ARM64 시스템 에뮬레이터 실행 파일이 설치되어 있고 게스트 이미지와 기계 유형이 호환될 때의 기본 예시입니다.

```sh
qemu-system-aarch64 --help
```

ARM64 게스트는 펌웨어, 커널, 기계 유형 및 부팅 방식이 필요할 수 있습니다. x86_64용 ISO를 이 명령에 그대로 넣는다고 ARM64로 바뀌지는 않습니다.

## 8. 안전한 종료

게스트 운영체제 안에서 정상 종료를 먼저 사용하세요. QEMU 터미널에서 종료 키 조합이나 모니터 명령을 사용하는 방법은 실행 방식과 설정에 따라 달라질 수 있습니다.

## 9. 부팅 메뉴 프로젝트에 관한 주의

QEMU 명령어는 가상 머신을 실행하는 도구입니다. Debian·Ubuntu·Windows 설치 항목을 한 메뉴에 모으려면, 각 항목이 어느 QEMU 실행 명령과 어느 ISO/가상 디스크를 사용할지 별도로 구성해야 합니다. 이것만으로 실제 기기의 부트로더가 교체되지는 않습니다.

- 각 OS는 별도 가상 디스크 파일을 사용하는 것이 안전합니다.
- Windows 버전별 설치 가능성은 ISO, 펌웨어, 가상 하드웨어, 라이선스 및 성능 조건에 따라 다릅니다.
- Galaxy A16의 RAM·발열·저장 공간 때문에 데스크톱 OS 가상화가 느리거나 실행되지 않을 수 있습니다.
- 이 문서는 명령어 참고용이며, 특정 명령의 휴대폰 실행 성공을 보증하지 않습니다.
