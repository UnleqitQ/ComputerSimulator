// Method: random_qword
// Param: none
// Return: value (rax)
$random_qword:
  // get random value
  in qword 0x0, qword 0x1004, rax;
  ret;
