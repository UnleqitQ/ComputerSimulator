jmp $main;

@include "time";
@include "screen";

/* main */

$main:
  /* allocate stack memory: duration [dword], start time [qword] */
  sub rsp, qword 0xc;
  /* set duration to 100 */
  mov dword [rsp + 0x8], dword 100;

  /* main loop: r8 */
  /* x loop: r9 */
  /* y loop: r10 */
  mov r8, qword 0;
  $loop_main:
    /* get start time */
    call $get_millis;
    mov qword [rsp], rdx;

    mov r9, qword 0;
    $loop_x:
      mov r10, qword 0;
      $loop_y:
        /* draw */
        /*x*/
        mov rax, r9;
        push ax;
        /*y*/
        mov rax, r10;
        push ax;
        /*red*/
        mov rax, r9;
        mul rax, word 255;
        div rax, word 19;
        push al;
        /*green*/
        mov rax, r10;
        mul rax, word 255;
        div rax, word 19;
        push al;
        /*blue*/
        mov rax, r8;
        mul rax, word 255;
        div rax, word 9;
        push al;
        /*draw*/
        call $screen_draw;
        /* end draw */
        inc r10;
        cmp r10, qword 20;
        jl $loop_y;
      inc r9;
      cmp r9, qword 20;
      jl $loop_x;
    inc r8;

    /* sleep duration */
    /* get end time */
    call $get_millis;
    /* end time - start time */
    sub rdx, qword [rsp];
    /* duration - elapsed time */
    mov eax, dword [rsp + 0x8];
    sub eax, edx;
    /* sleep */
    push eax;
    call $sleep;
    /* end sleep */

    jmp $loop_main;

  /* exit */
  int 0x0;
