@pragma once;

/* Method: get_millis */
/* Param: */
/* Return: millis (rdx) */
$get_millis:
  /* system port: 0x0, millis address: 0x210 */
  in qword 0x0, qword 0x210, rdx;
  ret;


/* Method: sleep */
/* Param: millis (dword) */
$sleep:
  pusha;

  /* calculate end time */
  in qword 0x0, qword 0x210, rax;
  mov rdx, qword 0;
  mov edx, dword [rbp + 0x10];
  /* check if negative */
  cmp edx, dword 0;
  jge $sleep_positive;
    popa;
    ret 4;

  $sleep_positive:
  add rdx, rax;

  $sleep_loop:
    in qword 0x0, qword 0x210, rax;
    cmp rax, rdx;
    jl $sleep_loop;

  popa;
  ret 4;
