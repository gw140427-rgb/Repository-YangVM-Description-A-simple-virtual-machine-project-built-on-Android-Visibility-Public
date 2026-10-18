# QEMU 명령어 전체 참고서 — Termux / YangVM

## Termux에서 QEMU 설치하기 (Android)

> 이 절은 설치 절차 안내다. 패키지 저장소와 기기 아키텍처에 따라 제공되는 패키지명이 달라질 수 있으므로, 설치 전에 저장소 검색 결과를 확인한다. 설치는 이 문서를 작성하는 과정에서 사용자의 휴대폰에 직접 실행한 것이 아니다.

### 1. 패키지 목록 갱신

```sh
pkg update
pkg upgrade
```

`pkg upgrade`는 다른 설치 패키지도 업데이트할 수 있으니, 변경 내용을 읽고 진행한다.

### 2. QEMU 관련 패키지 검색

```sh
pkg search qemu
pkg search qemu-system
pkg search qemu-img
```

### 3. 패키지 설치

검색 결과에 실제로 표시되는 이름만 선택해 설치한다. Termux 저장소에 아래 패키지가 표시된다면 다음과 같이 설치할 수 있다.

```sh
pkg install qemu-system-aarch64-headless qemu-system-x86-64-headless qemu-utils
```

**중요:** 위 세 패키지가 모든 Termux 환경에 제공된다고 보장할 수 없다. 패키지 하나가 “Unable to locate package” 오류를 내면 그 이름을 임의로 바꾸지 말고 `pkg search qemu` 결과에서 현재 저장소가 제공하는 정확한 패키지명을 확인한다. 그래픽 창이 필요한 패키지와 headless 패키지는 구성이 다를 수 있다.

### 4. 설치 확인

```sh
command -v qemu-system-aarch64
command -v qemu-system-x86_64
command -v qemu-img
qemu-system-aarch64 --version
qemu-system-x86_64 --version
qemu-img --version
```

실행 파일이 설치되지 않은 경우 해당 `command -v`는 경로를 출력하지 않는다. QEMU는 설치 패키지에 따라 실행 파일 이름과 제공 도구가 달라질 수 있다.

### 5. 저장 공간과 실행 환경 확인

```sh
df -h
free -h
uname -m
```

Galaxy A16 같은 ARM64 Android 기기에서 x86_64 운영체제를 실행하면 일반적으로 CPU 소프트웨어 에뮬레이션 비용이 커질 수 있다. KVM을 사용할 수 없는 환경에서는 하드웨어 가속을 가정하지 않는다. 설치된 빌드가 지원하는 기능은 각 실행 파일의 `--help`로 확인한다.

공식/참고 링크:
- Termux 패키지 저장소: https://github.com/termux/termux-packages
- Termux 패키지 이슈 및 최신 사용자 보고(공식 저장소 이슈): https://github.com/termux/termux-packages/issues/30743
- QEMU 다운로드/안정판: https://www.qemu.org/download

---


마지막 확인: 2026-10-18  
목표: QEMU 시스템 에뮬레이터와 관련 도구의 주요 명령·옵션을 한곳에 모으고, 공식 문서의 전체 옵션 색인을 함께 제공한다.

## 최신 버전 기준 (확인한 범위)

- 공식 QEMU 홈페이지 검색 결과에 표시된 최신 안정판: **11.1.2 (2026-09-28)**.
- 공식 개발 문서 `/docs/master/`는 개발 중인 문서이며, 검색 결과의 문서 버전은 **11.1.50**으로 표시된다. 이는 안정판 11.1.2와 다르다.
- 안정판 다운로드 및 릴리스는 아래 공식 페이지에서 다시 확인한다.
  - https://www.qemu.org/
  - https://www.qemu.org/download/
  - https://www.qemu.org/2026/08/11/qemu-11-1-0/
- Termux 패키지는 공식 PC용 QEMU 릴리스와 버전·빌드 옵션·실행 파일 구성이 같다고 가정하면 안 된다. 아래의 `--help` 및 `-version`으로 **실제 설치된 버전**을 확인한다.

## 공식 전체 문서 색인

QEMU는 대상 아키텍처, 빌드 옵션, 버전에 따라 수많은 옵션과 장치를 제공하므로, 하나의 고정된 목록만으로 모든 빌드의 모든 옵션을 정확히 나열할 수는 없다. 아래는 주요 명령과 옵션의 실용적인 색인이고, 전체 옵션은 해당 설치본의 도움말 및 공식 색인에서 확인한다.

- 시스템 에뮬레이터 개요: https://www.qemu.org/docs/master/system/introduction.html
- 시스템 에뮬레이터 전체 명령줄 옵션: https://www.qemu.org/docs/master/system/invocation.html
- QEMU 매뉴얼 페이지/옵션 색인: https://www.qemu.org/docs/master/system/qemu-manpage.html
- 지원 대상 아키텍처: https://www.qemu.org/docs/master/system/targets.html
- 장치 에뮬레이션: https://www.qemu.org/docs/master/system/devices/
- QEMU 모니터(HMP): https://www.qemu.org/docs/master/system/monitor.html
- QMP 명령·이벤트 전체 참조: https://www.qemu.org/docs/master/interop/qemu-qmp-ref.html
- 디스크 이미지 형식: https://www.qemu.org/docs/master/system/images.html
- `qemu-img`: https://www.qemu.org/docs/master/tools/qemu-img.html
- `qemu-nbd`: https://www.qemu.org/docs/master/tools/qemu-nbd.html
- `qemu-storage-daemon`: https://www.qemu.org/docs/master/tools/qemu-storage-daemon.html
- 전체 도구 목록: https://www.qemu.org/docs/master/tools/
- Termux 패키지 저장소: https://github.com/termux/termux-packages

## 1. 설치본·실행 파일·지원 옵션 확인

```sh
command -v qemu-system-x86_64
command -v qemu-system-aarch64
command -v qemu-img
command -v qemu-io
command -v qemu-nbd
qemu-system-x86_64 --version
qemu-system-x86_64 --help
qemu-system-aarch64 --version
qemu-system-aarch64 --help
qemu-img --version
qemu-img --help
```

일부 실행 파일은 설치되어 있지 않을 수 있다. 해당 명령이 없으면 현재 Termux 저장소의 패키지 이름·아키텍처·의존성을 확인한다. 위의 모든 실행 파일이 모든 Termux 빌드에 포함된다는 뜻은 아니다.

## 2. 가상 머신 실행 — 기본 예시

### ISO 설치 프로그램 부팅 (x86_64 에뮬레이션)

```sh
qemu-system-x86_64 -m 1024 -cdrom ./installer.iso -boot order=d
```

### ISO와 빈 가상 디스크로 설치

```sh
qemu-img create -f qcow2 ./debian.qcow2 16G
qemu-system-x86_64 -m 1024 -cdrom ./installer.iso -drive file=./debian.qcow2,format=qcow2 -boot order=d
```

### 설치된 가상 디스크로 부팅

```sh
qemu-system-x86_64 -m 1024 -drive file=./debian.qcow2,format=qcow2
```

### ARM64 에뮬레이터 실행 파일 도움말

```sh
qemu-system-aarch64 --help
```

ARM64 게스트는 적절한 기계 유형, 펌웨어/부트로더, 커널 및 이미지가 필요할 수 있다. x86_64 ISO를 ARM64로 자동 변환해 주지는 않는다.

## 3. 시스템 에뮬레이터 주요 옵션

아래는 흔히 쓰는 옵션의 용도다. 옵션 조합의 유효성은 에뮬레이터 종류와 빌드에 따라 다르다.

### 시스템·CPU·메모리
- `-machine` / `-M`: 가상 기계 유형과 기계 속성 지정.
- `-cpu`: 게스트 CPU 모델 지정.
- `-accel`: 가속기 선택(예: 빌드/호스트가 지원하는 경우 KVM, TCG).
- `-m`: 게스트 메모리 크기.
- `-smp`: 게스트 CPU/코어/스레드 구성.
- `-numa`: NUMA 토폴로지 구성.
- `-rtc`: 게스트 RTC 설정.
- `-icount`: 명령어 카운트 기반 실행/타이밍 옵션.
- `-name`: 가상 머신 이름.
- `-uuid`: 게스트 UUID.
- `-nodefaults`: 기본 장치 생성을 억제.
- `-no-user-config`: 사용자 설정 파일 자동 적용 억제.
- `-readconfig`, `-writeconfig`: 설정 파일 읽기/쓰기.

### 부팅·펌웨어·커널
- `-boot`: 부팅 순서 및 부팅 관련 옵션.
- `-bios`: BIOS 이미지 지정(지원되는 기계 유형에서).
- `-pflash`: 플래시 장치 이미지 연결.
- `-kernel`: 커널 이미지 직접 지정.
- `-initrd`: initrd/initramfs 이미지 지정.
- `-append`: 직접 부팅할 커널의 명령줄 인자.
- `-dtb`: Device Tree Blob 지정(지원 대상에서).
- `-no-reboot`: 게스트 재부팅 요청 시 종료 동작.
- `-no-shutdown`: 게스트 종료 요청 후 QEMU를 계속 유지하는 동작.

### 디스크·저장장치
- `-drive`: 간편 디스크/블록 장치 연결.
- `-blockdev`: 블록 계층을 명시적으로 구성.
- `-cdrom`: CD-ROM 이미지 연결(간편 옵션).
- `-hda`, `-hdb`, `-hdc`, `-hdd`: 레거시 IDE 디스크 간편 옵션; 현대적 구성에는 `-drive` 또는 `-blockdev` 사용을 고려.
- `-fda`, `-fdb`: 플로피 이미지 간편 옵션(지원되는 기계 유형에서).
- `-snapshot`: 쓰기 변경 사항을 임시 스냅샷 계층에 기록.
- `-drive ... ,format=qcow2`: 이미지 형식을 명시.
- `-device`: 저장 컨트롤러 또는 가상 장치 연결.

### 장치·USB·입력
- `-device`: 가상 장치 추가(장치 모델과 속성은 기계 유형/빌드에 따라 다름).
- `-usb`: USB 지원 활성화(해당 구성에서).
- `-device usb-...`: 지원되는 USB 컨트롤러·장치 모델 구성.
- `-display`: 그래픽 표시 방식 선택.
- `-vga`: 가상 VGA 장치 선택(기계 유형에 따라 다름).
- `-parallel`, `-serial`: 병렬/직렬 포트 백엔드 연결.
- `-chardev`: 직렬·모니터 등 문자 장치 백엔드 설정.
- `-mon`: 모니터 연결 방식 지정.

### 네트워크
- `-netdev`: 네트워크 백엔드 정의.
- `-device`: 가상 네트워크 카드 연결.
- `-nic`: 지원되는 빌드에서 네트워크 백엔드와 카드를 간편 설정.
- `-net`: 구형 네트워크 옵션; 새 구성에서는 `-nic`, `-netdev`, `-device`를 우선 검토.
- 사용자 모드 네트워크, TAP, 브리지 등은 호스트 권한과 빌드 지원에 따라 가능 여부가 다르다.

### 화면·원격 표시·모니터
- `-display`: 그래픽 표시 백엔드 설정.
- `-vnc`: VNC 서버 표시 백엔드 설정(빌드 지원 필요).
- `-spice`: SPICE 표시 백엔드 설정(빌드 지원 필요).
- `-nographic`: 그래픽 창 없이 콘솔 중심으로 실행.
- `-monitor`: HMP 모니터 연결.
- `-qmp`: QMP 제어 인터페이스 연결.
- `-serial`: 직렬 콘솔 출력 연결.
- `-S`: 시작 시 게스트 CPU 실행을 정지.
- `-gdb`: GDB 디버깅 연결 설정(정확한 구문은 도움말 참조).

### 디버깅·로그·운영
- `-d`: 디버그 로그 항목 지정.
- `-D`: 디버그 로그 파일 지정.
- `-trace`: 추적 이벤트 설정.
- `-pidfile`: PID 파일 경로.
- `-daemonize`: 지원 환경에서 백그라운드 실행.
- `-incoming`: 마이그레이션 수신 상태 설정.
- `-object`: QEMU 객체 생성.
- `-global`: 장치 전역 속성 설정.
- `-fw_cfg`: 펌웨어 구성 데이터 전달.
- `-sandbox`: 지원되는 빌드에서 제한된 실행 정책 설정.

정확한 인수 형식과 전체 속성은 공식 명령줄 참조 및 현재 실행 파일의 `--help`를 기준으로 한다. 예시를 그대로 복사하기 전에 파일 경로와 기계 유형을 확인한다.

## 4. qemu-img 명령어

`qemu-img`는 VM이 실행 중일 때가 아니라 **오프라인 이미지 관리**에 사용하는 것이 원칙이다. 실행 중인 가상 디스크에 이미지 수정 명령을 실행하면 손상될 수 있다.

### 하위 명령
- `create`: 새 이미지 생성.
- `info`: 이미지 형식·가상 크기 등 정보 표시.
- `check`: 지원되는 형식의 이미지 일관성 검사.
- `compare`: 두 이미지 내용 비교.
- `convert`: 이미지 형식 변환 또는 이미지 복사.
- `resize`: 가상 디스크 크기 변경.
- `snapshot`: 지원 형식의 내부 스냅샷 나열·생성·삭제.
- `rebase`: 백킹 파일 관계 변경.
- `commit`: 변경 내용을 백킹 파일로 병합.
- `map`: 이미지의 할당/매핑 정보 출력.
- `measure`: 변환/생성에 필요한 예상 저장 공간 계산.
- `amend`: 지원되는 이미지 형식 옵션 변경.
- `dd`: 원시 데이터 복사.
- `bench`: 이미지 I/O 벤치마크(해당 버전에서 제공되는 경우).

### 예시
```sh
qemu-img create -f qcow2 ./disk.qcow2 16G
qemu-img info ./disk.qcow2
qemu-img check ./disk.qcow2
qemu-img convert -p -f qcow2 -O raw ./disk.qcow2 ./disk.raw
qemu-img resize ./disk.qcow2 +4G
qemu-img snapshot -l ./disk.qcow2
qemu-img --help
```

주의: `resize`는 가상 디스크 용량만 변경할 수 있으며, 게스트 파티션/파일시스템 크기까지 자동으로 늘려 주는 것은 아니다. `convert`, `rebase`, `commit`, `resize` 전에는 백업을 권장한다.

## 5. 관련 QEMU 도구

도구가 설치된 경우에만 사용할 수 있다. 각 도구의 옵션은 해당 도움말을 확인한다.

```sh
qemu-img --help
qemu-io --help
qemu-nbd --help
qemu-storage-daemon --help
```

- `qemu-img`: 디스크 이미지 생성·검사·변환·크기 변경 등.
- `qemu-io`: 블록 장치/이미지 I/O 테스트.
- `qemu-nbd`: NBD 프로토콜로 이미지 내보내기.
- `qemu-storage-daemon`: QEMU 저장장치 기능을 별도 프로세스로 제공.
- 빌드/플랫폼에 따라 추가 도구가 제공될 수 있다. 전체 목록: https://www.qemu.org/docs/master/tools/

## 6. QEMU 모니터 명령

QEMU 모니터에는 HMP(사람이 입력하는 모니터 명령)와 QMP(기계가 사용하는 JSON 기반 프로토콜)가 있다. 아래는 HMP의 대표 예시이며, **모든 HMP/QMP 명령의 정확한 전체 목록은 공식 참조 문서**를 사용한다.

대표 HMP 명령:
- `help`: 사용 가능한 명령 도움말.
- `info status`: VM 실행/정지 상태.
- `info version`: QEMU 버전.
- `info block`: 블록 장치 정보.
- `info network`: 네트워크 정보(지원되는 경우).
- `info chardev`: 문자 장치 정보.
- `info pci`: PCI 장치 정보(기계 유형에 따라).
- `stop`: 게스트 CPU 정지.
- `cont`: 게스트 CPU 실행 재개.
- `system_reset`: 게스트 시스템 리셋.
- `system_powerdown`: 게스트에 전원 종료 요청.
- `quit`: QEMU 종료.
- `savevm`, `loadvm`, `delvm`: 지원되는 설정에서 VM 스냅샷 저장·복원·삭제.

공식 HMP 문서: https://www.qemu.org/docs/master/system/monitor.html  
공식 QMP 전체 명령 참조: https://www.qemu.org/docs/master/interop/qemu-qmp-ref.html

모니터 명령은 실행 중인 VM에 영향을 줄 수 있으므로, 목적을 이해한 뒤 사용한다. QMP는 HMP 명령과 문법이 같지 않다.

## 7. YangVM 멀티부팅 메뉴 설계 메모

QEMU 실행 명령은 가상 머신을 실행하는 것이며, 실제 휴대폰의 부트로더를 교체하는 작업이 아니다. OS 선택 메뉴는 각 항목에 ISO 경로, QEMU 실행 파일/아키텍처, 메모리, 기계 유형, 가상 디스크 및 필요한 펌웨어를 연결하는 별도 프로그램으로 설계해야 한다.

- Debian·Ubuntu·Windows 설치 항목은 가능하면 각각 별도 가상 디스크를 사용한다.
- x86_64 게스트를 ARM64 휴대폰에서 실행하면 대개 소프트웨어 에뮬레이션이 필요해 느릴 수 있다.
- Android 실제 파티션을 쓰기 가능 디스크로 연결하지 않는다.
- VM 파일·ISO를 삭제하거나 변환하기 전에 백업한다.
- Galaxy A16에서의 실행 성공이나 속도는 이 문서에서 검증하지 않았다.
- 모든 QEMU 옵션/장치가 Termux 패키지에 포함된다는 뜻은 아니다. 현재 설치본의 도움말이 우선이다.
