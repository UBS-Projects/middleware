//package com.middleware.backend.scheduledJobs.service;
//
//import com.middleware.backend.scheduledJobs.DTO.JobExecutionDTO;
//import com.middleware.backend.scheduledJobs.mapper.LogsMapper;
//import com.middleware.backend.scheduledJobs.repository.LogsRepository;
//import lombok.AllArgsConstructor;
//import org.springframework.stereotype.Service;
//
//@Service
//@AllArgsConstructor
//public class JobLogsService {
//    private final LogsRepository repo;
//
//
//    public void save(JobExecutionDTO job){
//        repo.save(LogsMapper.MapToDto(job));
//    }
//}
