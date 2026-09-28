# YangVM Tiny CPU

YangVM is a tiny educational virtual CPU emulator designed to run in a mobile browser.

## Version 0.1
- 8-bit registers A and B (values wrap from 0 to 255)
- 256 bytes of RAM (the UI displays the first 16 cells)
- Step-by-step execution and run-to-halt
- Output, program counter, and execution log

## Instructions
- `LOAD A, 5` — put a number in a register
- `ADD A, B` / `SUB A, B` — arithmetic (8-bit wraparound)
- `STORE 12, A` — save a register value to RAM address 0–255
- `PRINT A` — show a register value
- `JMP 0` — jump to a zero-based instruction index
- `JNZ A, 2` — jump to instruction 2 if A is not zero
- `HALT` — stop

## Run
Open `index.html` in a modern browser. No build step, account, or paid service is required.

## Scope
This is a real instruction-executing toy emulator, not hardware virtualization. It cannot boot Android, Debian, Windows, or standard disk images. It is a foundation for learning how CPUs, instructions, registers, and memory work.
