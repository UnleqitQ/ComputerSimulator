@pragma once;

/* Method: heapsize */
/* Parameters: */
/* Return: size [rax] */
$heapsize:
  /* System call to get the size of the memory */
  /* Device: 0x0, Command: 0x100 */
  in word 0x0, word 0x100, rax;
  /* only a third of the memory is used for the heap */
  div rax, qword 3;
  ret;



/* Method: malloc */
/* Parameters: size [word] */
/* Return: address [rax], success [bl] */
$malloc:
  /* push registers */
  push rcx;
  push rdx;
  push r8;
  push r9;
  push r10;
  push r11;

  /* get size of allocatable memory */
  call $heapsize;
  mov rcx, rax;
  sub rcx, qword 256; /* 256 bytes are reserved for malloc itself */
  /* get size of requested memory */
  mov dx, word ss:[rbp+0x10];
  /* align size to 16 bytes */
  add dx, qword 0xF;
  shr dx, qword 4;

  sub rsp, qword 10;
  mov word ss:[rsp], dx; /* store requested size */
  mov qword ss:[rsp+2], rcx; /* store heap size */

  /* in the following, block stands for a 16 byte sequence */
  /* find free memory sequence */
  /* iterate over memory blocks, in the 256 bytes reserved for malloc itself, the bytes contain bit flags, each bit represents a 16 byte block */
  /* if a block is free, the bit is set to 0, if it is allocated, the bit is set to 1 */
  /* r8 is used to store the start of the sequence, that is known to be free */
  /* r9 is used to store the end of the sequence, that is known to be free */
  /* r9 is being updated in each iteration */
  /* if r8 or r9 is not free, r8 is set to r9 */
  mov r8, qword 0;
  mov r9, qword 0;
  $malloc_int_loop:

    /* check if r9 is at the end of the heap */
    cmp r9, qword ss:[rsp+0x2];
    jge $malloc_int_fail;

    /* check if the block at r9 is free (push r9 (parameter) and call malloc_sub_check) */
    push r9;
    call $malloc_sub_check;
    test al, al;
    jnz $malloc_int_reset;
    /* r9 is free */
    /* check if the block at r8 is free (push r8 (parameter) and call malloc_sub_check) */
    push r8;
    call $malloc_sub_check;
    test al, al;
    jz $malloc_int_no_reset;
      /* r8 is not free, set r8 to r9 */
      mov r8, r9;
    $malloc_int_no_reset:
    /* r8 is free, that means r8 through r9 is free */
    mov r10, r9;
    sub r10, r8;
    add r10, qword 1;
    cmp r10, word ss:[rsp];
    /* if greater or equal, the sequence is large enough */
    jge $malloc_int_end;
    /* if not, increase r9 and continue */
    inc r9;
    jmp $malloc_int_loop;
    $malloc_int_reset:
      /* r9 is not free, set r8 to r9 */
      inc r9;
      mov r8, r9;
      jmp $malloc_int_loop;
  /* end of loop */

  $malloc_int_end:
    /* r8 through r9 is free and large enough */
    /* mark the block as allocated */
    mov r10, r8;
    mov r11, r9;
    $malloc_int_mark_loop:
      push r10;
      push byte 1;
      call $malloc_sub_mark;
      inc r10;
      cmp r10, r11;
      jle $malloc_int_mark_loop;
    /* return the address of the block */
    mov rax, r8;
    shl rax, qword 4;
    add rax, qword 256;
    mov bl, byte 1;

    add rsp, qword 10;
    /* pop registers */
    pop r11;
    pop r10;
    pop r9;
    pop r8;
    pop rdx;
    pop rcx;
    ret 2; /* 1 word parameter (2 bytes) */

  $malloc_int_fail:
    /* no free memory sequence large enough */
    mov rax, qword 0;
    mov bl, byte 0;

    add rsp, qword 10;
    /* pop registers */
    pop r11;
    pop r10;
    pop r9;
    pop r8;
    pop rdx;
    pop rcx;
    ret 2; /* 1 word parameter (2 bytes) */



/* Method: malloc_sub_check */
/* Parameters: block [qword] */
/* Return: allocated [al] */
$malloc_sub_check:
  push rbx;
  push rcx;
  push rdx;
  /* rbx contains the byte index of the block */
  mov rbx, qword ss:[rbp+0x10];
  /* rcx contains the bit index of the block */
  mov rcx, rbx;
  and rcx, qword 0x7;
  shr rbx, qword 3;

  /* check if the block is free */
  mov al, byte [rbx];
  mov dl, byte 1;
  shl dl, cl;
  test al, dl;
  jnz $malloc_sub_check_fail;
    mov al, byte 0;
  jmp $malloc_sub_check_end;
  $malloc_sub_check_fail:
    mov al, byte 1;
  $malloc_sub_check_end:

  pop rdx;
  pop rcx;
  pop rbx;
  ret 8; /* 1 qword parameter (8 bytes) */



/* Method: malloc_sub_mark */
/* Parameters: block [qword], value [byte] (0 or 1) */
$malloc_sub_mark:
  push rax;
  push rbx;
  push rcx;
  push rdx;
  /* rbx contains the byte index of the block */
  mov rbx, qword ss:[rbp+0x11];
  /* rcx contains the bit index of the block */
  mov rcx, rbx;
  and rcx, qword 0x7;
  shr rbx, qword 3;

  /* get the value to set */
  mov al, byte ss:[rbp+0x10];
  mov dl, byte 1;
  shl dl, cl;
  /* dl contains the bit mask */
  /* save the byte in cl */
  mov cl, byte [rbx];
  test al, al;
  jz $malloc_sub_mark_clear;
    or cl, dl;
    jmp $malloc_sub_mark_end;
  $malloc_sub_mark_clear:
    not dl;
    and cl, dl;
  $malloc_sub_mark_end:
  mov byte [rbx], cl;

  pop rdx;
  pop rcx;
  pop rbx;
  pop rax;
  ret 9; /* 1 qword parameter (8 bytes), 1 byte parameter (1 byte) */



/* Method: free */
/* Parameters: address [qword], size [word] */
/* Not checking if the address or size is valid, just marking the block as free */
$free:
  /* push registers */
  push rax;
  push rbx;

  /* get address */
  mov rax, qword ss:[rbp+0x12];
  sub rax, qword 256;
  shr rax, qword 4;
  /* get size */
  mov rbx, word ss:[rbp+0x10];
  /* align size to 16 bytes */
  add rbx, qword 0xF;
  shr rbx, qword 4;

  /* store in rbx the end of the block */
  add rbx, rax;

  /* mark the blocks as free */
  $free_loop:
    push rax;
    push byte 0;
    call $malloc_sub_mark;
    inc rax;
    cmp rax, rbx;
    jl $free_loop;

  /* pop registers */
  pop rbx;
  pop rax;
  ret 10; /* 1 qword parameter (8 bytes), 1 word parameter (2 bytes) */

