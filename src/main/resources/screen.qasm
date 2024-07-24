@pragma once;

// Method: screen_draw
// Param: x (word), y (word), red (byte), green (byte), blue (byte)
$screen_draw:
  pusha;

  mov rax, qword 0;

  // load x
  mov rbx, qword 0;
  mov bx, word [rbp + 0x15];
  or rax, rbx;

  // load y
  mov rbx, qword 0;
  mov bx, word [rbp + 0x13];
  shl rbx, qword 16;
  or rax, rbx;

  // load color
  mov rbx, qword 0;
  // red
  mov bl, byte [rbp + 0x12];
  shl rbx, qword 8;
  // green
  mov bl, byte [rbp + 0x11];
  shl rbx, qword 8;
  // blue
  mov bl, byte [rbp + 0x10];
  // apply
  shl rbx, qword 32;
  or rax, rbx;

  // send to screen
  // default port: 0x8020, draw address: 0x0
  out qword 0x8020, qword 0x0, rax;

  popa;
  // 2 words + 3 bytes = 7 bytes
  ret 7;

// Method: screen_clear
// Param: none
$screen_clear:
  // send to screen
  // default port: 0x8020, clear address: 0x1
  out qword 0x8020, qword 0x1, byte 0x0;
  ret;
