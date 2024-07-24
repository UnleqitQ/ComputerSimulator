jmp $main;

@include <screen>;
@include <time>;
@include <random>;

// Method: handle_input
// Param: address (r15)
// left: 37, up: 38, right: 39, down: 40
// keyboard port: 0x8000
// address:
//  in - get: 0x10
//  in - buffer size: 0x11
//  out - clear: 0x10
$handle_input:
  pusha;
  in qword 0x8000, qword 0x11, r8;
  $handle_input_loop:
    cmp r8, byte 0;
    jz $handle_input_end;
    dec r8;
    in qword 0x8000, qword 0x10, r10;
    mov rax, r10;
    mov rbx, qword 0;
    not rbx;
    shl rbx, qword 31;
    not rbx;
    and rax, rbx;
    mov r9, rax;
    mov rax, r10;
    shr rax, qword 31;
    // if 1 then key down
    test rax, qword 1;
    jz $handle_input_loop;
    cmp r9, byte 37;
    jz $handle_input_left;
    cmp r9, byte 38;
    jz $handle_input_up;
    cmp r9, byte 39;
    jz $handle_input_right;
    cmp r9, byte 40;
    jz $handle_input_down;
    jmp $handle_input_loop;
    $handle_input_left:
    mov al, byte 3;
    call $set_direction;
    jmp $handle_input_loop;
    $handle_input_up:
    mov al, byte 0;
    call $set_direction;
    jmp $handle_input_loop;
    $handle_input_right:
    mov al, byte 1;
    call $set_direction;
    jmp $handle_input_loop;
    $handle_input_down:
    mov al, byte 2;
    call $set_direction;
    jmp $handle_input_loop;
  $handle_input_end:
  out qword 0x8000, qword 0x10, byte 0;
  popa;
  ret;

// Method: get_size
// Param: address (r15)
// Return: width (al), height (ah)
$get_size:
  mov rax, qword 0;
  mov al, byte [r15 + 0x0];
  mov ah, byte [r15 + 0x1];
  ret;

// Method: set_size
// Param: address (r15), width (al), height (ah)
$set_size:
  mov byte [r15 + 0x0], al;
  mov byte [r15 + 0x1], ah;
  ret;

// Method: get_food
// Param: address (r15)
// Return: x (al), y (ah)
$get_food:
  mov rax, qword 0;
  mov al, byte [r15 + 0x2];
  mov ah, byte [r15 + 0x3];
  ret;

// Method: set_food
// Param: address (r15), x (al), y (ah)
$set_food:
  mov byte [r15 + 0x2], al;
  mov byte [r15 + 0x3], ah;
  ret;

// Method: move_food
// Param: address (r15)
$move_food:
  pusha;
  // get size
  call $get_size;
  mov rbx, qword 0;
  mov bl, al;
  mov rcx, qword 0;
  mov cl, ah;
  // new x
  call $random_qword;
  mod rax, rbx;
  mov rbx, rax;
  // new y
  call $random_qword;
  mod rax, rcx;
  mov rcx, rax;
  // set food
  mov rax, qword 0;
  mov al, bl;
  mov ah, cl;
  call $set_food;
  popa;
  ret;

// Method: get_direction
// Param: address (r15)
// Return: direction (al)
// 0: up, 1: right, 2: down, 3: left
$get_direction:
  mov rax, qword 0;
  mov al, byte [r15 + 0x4];
  ret;

// Method: set_direction
// Param: address (r15), direction (al)
$set_direction:
  mov byte [r15 + 0x4], al;
  ret;

// Method: get_length
// Param: address (r15)
// Return: length (ax)
$get_length:
  mov rax, qword 0;
  mov ax, word [r15 + 0x5];
  ret;

// Method: set_length
// Param: address (r15), length (ax)
$set_length:
  mov word [r15 + 0x5], ax;
  ret;

// Method: get_part_position
// Param: address (r15), index (dx)
// Return: x (al), y (ah)
$get_part_position:
  mov rax, qword 0;
  mov al, byte [r15 + dx * 2 + 0x7];
  mov ah, byte [r15 + dx * 2 + 0x8];
  ret;

// Method: set_part_position
// Param: address (r15), index (dx), x (al), y (ah)
$set_part_position:
  mov byte [r15 + dx * 2 + 0x7], al;
  mov byte [r15 + dx * 2 + 0x8], ah;
  ret;

// Method: check_collision
// Param: address (r15), head x (al), head y (ah)
// Return: collision (al)
$check_collision:
  push rdx;
  sub rsp, qword 0x7;
  // x (byte, rsp + 0x0)
  mov byte [rsp + 0x0], al;
  // y (byte, rsp + 0x1)
  mov byte [rsp + 0x1], ah;
  // length (word, rsp + 0x2)
  call $get_length;
  sub ax, word 0x1;
  mov word [rsp + 0x2], ax;
  // counter (word, rsp + 0x4)
  mov word [rsp + 0x4], word 0;
  // collision (byte, rsp + 0x6)
  mov byte [rsp + 0x6], byte 0;
  // loop
  $check_collision_loop:
    // check loop end
    cmp word [rsp + 0x4], word [rsp + 0x2];
    jge $check_collision_end;
    // get part position
    mov dx, word [rsp + 0x4];
    call $get_part_position;
    // check collision
    cmp al, byte [rsp + 0x0];
    jnz $check_collision_next;
    cmp ah, byte [rsp + 0x1];
    jnz $check_collision_next;
    // collision
    mov byte [rsp + 0x6], byte 1;
    jmp $check_collision_end;
    // next
    $check_collision_next:
    inc word [rsp + 0x4];
    jmp $check_collision_loop;
  // end
  $check_collision_end:
  mov rax, qword 0;
  mov al, byte [rsp + 0x6];
  add rsp, qword 0x7;
  pop rdx;
  ret;

// Method: check_wall
// Param: address (r15), head x (al), head y (ah)
// Return: collision (al)
$check_wall:
  push rcx;
  push rdx;
  // write head in d reg
  mov rdx, qword 0;
  mov dx, ax;
  // get size in c reg
  call $get_size;
  mov rcx, qword 0;
  mov cx, ax;
  // clear rax, store in al collision
  mov rax, qword 0;
  // check x, (unsigned) so no need to check negative, when out of bounds, it will be greater than size
  cmp dl, cl;
  jae $check_wall_collision;
  // check y (as above)
  cmp dh, ch;
  jae $check_wall_collision;
  // no collision
  jmp $check_wall_end;
  // collision
  $check_wall_collision:
  mov al, byte 1;
  // end
  $check_wall_end:
  pop rdx;
  pop rcx;
  ret;

// Method: check_food
// Param: address (r15), head x (al), head y (ah)
// Return: collision (al)
$check_food:
  push rcx;
  push rdx;
  // write head in d reg
  mov rdx, qword 0;
  mov dx, ax;
  // get food in c reg
  call $get_food;
  mov rcx, qword 0;
  mov cx, ax;
  // clear rax, store in al collision
  mov rax, qword 0;
  // check x
  cmp dl, cl;
  jnz $check_food_end;
  // check y
  cmp dh, ch;
  jnz $check_food_end;
  // collision
  mov al, byte 1;
  // end
  $check_food_end:
  pop rdx;
  pop rcx;
  ret;

// Method: draw_snake
// Param: address (r15)
$draw_snake:
  pusha;
  // clear screen
  call $screen_clear;
  // draw food
  call $get_food;
  mov rbx, qword 0;
  mov rcx, qword 0;
  mov bl, al;
  mov cl, ah;
  push bx;
  push cx;
  push byte 0xff;
  push byte 0x0;
  push byte 0x0;
  call $screen_draw;
  // draw snake
  call $get_length;
  mov cx, ax;
  // loop var (dx)
  mov dx, word 0;
  $draw_snake_loop:
    // check loop condition
    cmp dx, cx;
    jge $draw_snake_loop_end;
    // get part position
    call $get_part_position;
    mov rbx, qword 0;
    // draw
    mov bl, al;
    push bx;
    mov bl, ah;
    push bx;
    push byte 0x0;
    push byte 0xff;
    push byte 0x0;
    call $screen_draw;
    // next
    inc dx;
    jmp $draw_snake_loop;
  $draw_snake_loop_end:
  popa;
  ret;

// Method: move_snake
// Param: address (r15)
// Return: collision (al)
$move_snake:
  pusha;
  sub rsp, qword 0x10;
  // get length (word, rsp + 0x0)
  call $get_length;
  mov word [rsp + 0x0], ax;
  // get direction (byte, rsp + 0x2)
  call $get_direction;
  mov byte [rsp + 0x2], al;
  // calculate new head position
  mov rdx, qword 0;
  call $get_part_position;
  // check up
  cmp byte [rsp + 0x2], byte 0;
  jnz $move_snake_head_right;
  dec ah;
  jmp $move_snake_head_end;
  // check right
  $move_snake_head_right:
  cmp byte [rsp + 0x2], byte 1;
  jnz $move_snake_head_down;
  inc al;
  jmp $move_snake_head_end;
  // check down
  $move_snake_head_down:
  cmp byte [rsp + 0x2], byte 2;
  jnz $move_snake_head_left;
  inc ah;
  jmp $move_snake_head_end;
  // check left
  $move_snake_head_left:
  dec al;
  // end
  $move_snake_head_end:
  // store
  mov word [rsp + 0x3], ax;
  // check collision
  call $check_collision;
  test al, al;
  jnz $move_snake_collision;
  // check wall
  mov ax, word [rsp + 0x3];
  call $check_wall;
  test al, al;
  jnz $move_snake_collision;
  // check food
  mov ax, word [rsp + 0x3];
  call $check_food;
  test al, al;
  jz $move_snake_no_food;
  // eat food
  call $move_food;
  // increase length
  call $get_length;
  inc ax;
  // update length in memory
  mov word [rsp + 0x0], ax;
  call $set_length;
  // end food
  $move_snake_no_food:
  // move parts
  // loop var (cx)
  mov cx, word [rsp + 0x0];
  $move_snake_parts_loop:
    // check loop condition
    cmp cx, word 0;
    jle $move_snake_parts_loop_end;
    // get next part position
    mov dx, cx;
    dec dx;
    call $get_part_position;
    mov dx, cx;
    call $set_part_position;
    // next
    dec cx;
    jmp $move_snake_parts_loop;
  $move_snake_parts_loop_end:
  // move head
  mov ax, word [rsp + 0x3];
  mov dx, word 0;
  call $set_part_position;
  // end
  add rsp, qword 0x10;
  popa;
  mov al, byte 0;
  ret;
  // collision
  $move_snake_collision:
  add rsp, qword 0x10;
  popa;
  mov al, byte 1;
  ret;

// Method: fill_red
// Param: address (r15)
$fill_red:
  pusha;
  // get size
  call $get_size;
  // loop x
  mov cl, qword 0;
  $fill_red_loop_x:
    cmp cl, al;
    jae $fill_red_loop_x_end;
    // loop y
    mov dl, qword 0;
    $fill_red_loop_y:
      cmp dl, ah;
      jae $fill_red_loop_y_end;
      // draw
      mov rbx, qword 0;
      mov bl, cl;
      push bx;
      mov bl, dl;
      push bx;
      push byte 0xff;
      push byte 0x0;
      push byte 0x0;
      call $screen_draw;
      // next y
      inc dl;
      jmp $fill_red_loop_y;
    $fill_red_loop_y_end:
    // next x
    inc cl;
    jmp $fill_red_loop_x;
  $fill_red_loop_x_end:
  popa;
  ret;

// Method: init_snake
// Param: address (r15)
$init_snake:
  pusha;
  // set length
  mov ax, word 3;
  call $set_length;
  // set direction
  mov al, byte 1;
  call $set_direction;
  // get size
  call $get_size;
  // set head to center
  div al, byte 2;
  div ah, byte 2;
  // set parts
  mov cx, word 0;
  $init_snake_loop:
    cmp cx, word 3;
    jge $init_snake_loop_end;
    mov dx, cx;
    call $set_part_position;
    inc cx;
    jmp $init_snake_loop;
  $init_snake_loop_end:
  popa;
  ret;

// Method: main
$main:
  sub rsp, qword 0x30;

  // set speed (in millis)
  mov dword [rsp + 0x0], dword 500;

  // init data
  mov r15, qword 0x0;
  mov qword [rsp + 0x8], r15;

  // set size
  mov rax, qword 0;
  mov al, byte 20; // width
  mov ah, byte 20; // height
  call $set_size;

  // set food
  call $move_food;

  // init snake
  call $init_snake;

  // main loop
  $main_loop:
    // get start time
    call $get_millis;
    mov qword [rsp + 0x10], rdx;

    // handle input
    mov r15, qword [rsp + 0x8];
    call $handle_input;

    // move snake
    mov r15, qword [rsp + 0x8];
    call $move_snake;
    test al, al;
    jnz $main_end;

    // draw snake
    mov r15, qword [rsp + 0x8];
    call $draw_snake;

    // get end time
    call $get_millis;
    mov qword [rsp + 0x18], rdx;

    // calculate elapsed time
    mov rax, qword [rsp + 0x18];
    sub rax, qword [rsp + 0x10];

    // calculate sleep time
    mov rdx, dword [rsp + 0x0];
    sub rdx, rax;

    // sleep
    push edx;
    call $sleep;

    // next loop
    jmp $main_loop;

  $main_end:
  mov r15, qword [rsp + 0x8];
  call $fill_red;

  add rsp, qword 0x30;

  int 0x0;
