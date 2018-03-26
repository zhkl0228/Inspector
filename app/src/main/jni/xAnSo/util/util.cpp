/******************************************************************************* 
 *  @file      util.cpp 2017\5\11 17:01:27 $
 *  @author    df
 *  @brief     
 ******************************************************************************/

#include "util.h"
#include <fstream>

/******************************************************************************/


/******************************************************************************/

#define INT_LEN (10)
#define HEX_LEN (8)
#define BIN_LEN (32)
#define OCT_LEN (11)

static char *  my_itoa ( int value, char * str, int base )
{
    int i,n =2,tmp;
    char buf[BIN_LEN+1];


    switch(base)
    {
        case 16:
            for(i = 0;i<HEX_LEN;++i)
            {
                if(value/base>0)
                {
                    n++;
                }
            }
            snprintf(str, n, "%x" ,value);
            break;
        case 10:
            for(i = 0;i<INT_LEN;++i)
            {
                if(value/base>0)
                {
                    n++;
                }
            }
            snprintf(str, n, "%d" ,value);
            break;
        case 8:
            for(i = 0;i<OCT_LEN;++i)
            {
                if(value/base>0)
                {
                    n++;
                }
            }
            snprintf(str, n, "%o" ,value);
            break;
        case 2:
            for(i = 0,tmp = value;i<BIN_LEN;++i)
            {
                if(tmp/base>0)
                {
                    n++;
                }
                tmp/=base;
            }
            for(i = 1 ,tmp = value; i<n;++i)
            {
                if(tmp%2 != 0)
                {
                    buf[n-i-1] ='1';
                }
                else
                {
                    buf[n-i-1] ='0';
                }
                tmp/=base;
            }
            buf[n-1] = '\0';
            strcpy(str,buf);
            break;
        default:
            return NULL;
    }
    return str;
}

std::string util::itoa(int i, int rdx)
{
    char _rs[100] = {0};
    my_itoa(i, _rs, rdx);

    return std::string(_rs);
}

std::string util::read_file(std::string file)
{
    std::string file_content;

    char *file_content_buf = nullptr;
    try{
        std::ifstream file_;
        file_.open(file, std::ios_base::binary);
        if (!file_.is_open())
        {
            return file_content;
        }
        file_.seekg(0, std::ios::end);
        std::streamoff file_size = file_.tellg();
        file_.seekg(0);
        file_content_buf = new char[(unsigned int)file_size];
        file_.read(file_content_buf, file_size);
        file_content = std::string(file_content_buf, (unsigned int)file_size);
        file_.close();
        delete[]file_content_buf;
        file_content_buf = nullptr;
    }
    catch (...){
        if (!file_content_buf){
            delete[]file_content_buf;
        }
    }
    return file_content;
}
